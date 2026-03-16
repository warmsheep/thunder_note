package com.flashnote.flashnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashnote.auth.entity.User;
import com.flashnote.auth.mapper.UserMapper;
import com.flashnote.common.exception.BusinessException;
import com.flashnote.common.response.ErrorCode;
import com.flashnote.flashnote.entity.FlashNote;
import com.flashnote.flashnote.mapper.FlashNoteMapper;
import com.flashnote.flashnote.service.FlashNoteService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlashNoteServiceImpl implements FlashNoteService {
    private final UserMapper userMapper;
    private final FlashNoteMapper flashNoteMapper;

    public FlashNoteServiceImpl(UserMapper userMapper, FlashNoteMapper flashNoteMapper) {
        this.userMapper = userMapper;
        this.flashNoteMapper = flashNoteMapper;
    }

    @Override
    public List<FlashNote> listNotes(String username) {
        Long userId = getRequiredUserId(username);
        return flashNoteMapper.selectList(new LambdaQueryWrapper<FlashNote>()
                .eq(FlashNote::getUserId, userId)
                .eq(FlashNote::getDeleted, false)
                .orderByDesc(FlashNote::getUpdatedAt));
    }

    @Override
    public FlashNote createNote(String username, FlashNote note) {
        Long userId = getRequiredUserId(username);
        note.setUserId(userId);
        note.setDeleted(false);
        flashNoteMapper.insert(note);
        return note;
    }

    @Override
    public FlashNote updateNote(String username, Long noteId, FlashNote incoming) {
        Long userId = getRequiredUserId(username);
        FlashNote note = flashNoteMapper.selectOne(new LambdaQueryWrapper<FlashNote>()
                .eq(FlashNote::getId, noteId)
                .eq(FlashNote::getUserId, userId)
                .eq(FlashNote::getDeleted, false));

        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Note not found");
        }

        note.setTitle(incoming.getTitle());
        note.setIcon(incoming.getIcon());
        note.setContent(incoming.getContent());
        note.setTags(incoming.getTags());
        flashNoteMapper.updateById(note);
        return note;
    }

    @Override
    public void deleteNote(String username, Long noteId) {
        Long userId = getRequiredUserId(username);
        FlashNote note = flashNoteMapper.selectOne(new LambdaQueryWrapper<FlashNote>()
                .eq(FlashNote::getId, noteId)
                .eq(FlashNote::getUserId, userId)
                .eq(FlashNote::getDeleted, false));
        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Note not found");
        }
        note.setDeleted(true);
        flashNoteMapper.updateById(note);
    }

    private Long getRequiredUserId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User not found");
        }
        return user.getId();
    }
}
