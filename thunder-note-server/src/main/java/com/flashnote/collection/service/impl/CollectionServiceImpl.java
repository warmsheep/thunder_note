package com.flashnote.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashnote.auth.entity.User;
import com.flashnote.auth.mapper.UserMapper;
import com.flashnote.collection.entity.Collection;
import com.flashnote.collection.mapper.CollectionMapper;
import com.flashnote.collection.service.CollectionService;
import com.flashnote.common.exception.BusinessException;
import com.flashnote.common.response.ErrorCode;
import com.flashnote.flashnote.entity.FlashNote;
import com.flashnote.flashnote.mapper.FlashNoteMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollectionServiceImpl implements CollectionService {
    private final UserMapper userMapper;
    private final CollectionMapper collectionMapper;
    private final FlashNoteMapper flashNoteMapper;

    public CollectionServiceImpl(UserMapper userMapper,
                                 CollectionMapper collectionMapper,
                                 FlashNoteMapper flashNoteMapper) {
        this.userMapper = userMapper;
        this.collectionMapper = collectionMapper;
        this.flashNoteMapper = flashNoteMapper;
    }

    @Override
    public List<Collection> listCollections(String username) {
        Long userId = getRequiredUserId(username);
        return collectionMapper.selectList(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getUserId, userId)
                .orderByDesc(Collection::getUpdatedAt));
    }

    @Override
    public Collection createCollection(String username, Collection collection) {
        Long userId = getRequiredUserId(username);
        collection.setUserId(userId);
        collectionMapper.insert(collection);
        return collection;
    }

    @Override
    public Collection updateCollection(String username, Long id, Collection incoming) {
        Long userId = getRequiredUserId(username);
        Collection collection = collectionMapper.selectOne(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getId, id)
                .eq(Collection::getUserId, userId));

        if (collection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Collection not found");
        }

        String oldName = collection.getName();
        collection.setName(incoming.getName());
        collection.setDescription(incoming.getDescription());
        collectionMapper.updateById(collection);
        cascadeRename(userId, oldName, incoming.getName());
        return collection;
    }

    @Override
    public void deleteCollection(String username, Long id) {
        Long userId = getRequiredUserId(username);
        Collection collection = collectionMapper.selectOne(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getId, id)
                .eq(Collection::getUserId, userId));

        if (collection == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Collection not found");
        }

        collectionMapper.delete(new LambdaQueryWrapper<Collection>()
                .eq(Collection::getId, id)
                .eq(Collection::getUserId, userId));
        cascadeClear(userId, collection.getName());
    }

    private void cascadeRename(Long userId, String oldName, String newName) {
        if (isBlank(oldName) || oldName.equals(newName)) {
            return;
        }
        List<FlashNote> notes = flashNoteMapper.selectList(new LambdaQueryWrapper<FlashNote>()
                .eq(FlashNote::getUserId, userId)
                .eq(FlashNote::getDeleted, false)
                .eq(FlashNote::getTags, oldName));
        for (FlashNote note : notes) {
            note.setTags(newName);
            flashNoteMapper.updateById(note);
        }
    }

    private void cascadeClear(Long userId, String collectionName) {
        if (isBlank(collectionName)) {
            return;
        }
        List<FlashNote> notes = flashNoteMapper.selectList(new LambdaQueryWrapper<FlashNote>()
                .eq(FlashNote::getUserId, userId)
                .eq(FlashNote::getDeleted, false)
                .eq(FlashNote::getTags, collectionName));
        for (FlashNote note : notes) {
            note.setTags(null);
            flashNoteMapper.updateById(note);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Long getRequiredUserId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User not found");
        }
        return user.getId();
    }
}
