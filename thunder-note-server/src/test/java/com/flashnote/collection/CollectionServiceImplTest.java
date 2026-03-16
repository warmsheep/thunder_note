package com.flashnote.collection;

import com.flashnote.auth.entity.User;
import com.flashnote.auth.mapper.UserMapper;
import com.flashnote.collection.entity.Collection;
import com.flashnote.collection.mapper.CollectionMapper;
import com.flashnote.collection.service.impl.CollectionServiceImpl;
import com.flashnote.common.exception.BusinessException;
import com.flashnote.common.response.ErrorCode;
import com.flashnote.flashnote.entity.FlashNote;
import com.flashnote.flashnote.mapper.FlashNoteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceImplTest {

    private UserMapper createMockUserMapper(Long userId, String username) {
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        when(userMapper.selectOne(any())).thenReturn(user);
        return userMapper;
    }

    @Test
    void updateCollection_renamesFlashNoteTags() {
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        CollectionMapper collectionMapper = mock(CollectionMapper.class);
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        Collection existing = new Collection();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setName("工作");
        when(collectionMapper.selectOne(any())).thenReturn(existing);

        FlashNote note = new FlashNote();
        note.setId(100L);
        note.setUserId(1L);
        note.setTitle("周会");
        note.setTags("工作");
        note.setDeleted(false);
        when(flashNoteMapper.selectList(any())).thenReturn(List.of(note));

        Collection incoming = new Collection();
        incoming.setName("项目");
        incoming.setDescription("desc");

        CollectionServiceImpl service = new CollectionServiceImpl(userMapper, collectionMapper, flashNoteMapper);

        Collection result = service.updateCollection("alice", 10L, incoming);

        assertEquals("项目", result.getName());
        assertEquals("项目", note.getTags());
        verify(flashNoteMapper).updateById(note);
    }

    @Test
    void deleteCollection_clearsFlashNoteTags() {
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        CollectionMapper collectionMapper = mock(CollectionMapper.class);
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        Collection existing = new Collection();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setName("工作");
        when(collectionMapper.selectOne(any())).thenReturn(existing);

        FlashNote note = new FlashNote();
        note.setId(100L);
        note.setUserId(1L);
        note.setTitle("周会");
        note.setTags("工作");
        note.setDeleted(false);
        when(flashNoteMapper.selectList(any())).thenReturn(List.of(note));

        CollectionServiceImpl service = new CollectionServiceImpl(userMapper, collectionMapper, flashNoteMapper);

        service.deleteCollection("alice", 10L);

        assertNull(note.getTags());
        verify(flashNoteMapper).updateById(note);
    }

    @Test
    void deleteCollection_missingCollectionThrows() {
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        CollectionMapper collectionMapper = mock(CollectionMapper.class);
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);
        when(collectionMapper.selectOne(any())).thenReturn(null);

        CollectionServiceImpl service = new CollectionServiceImpl(userMapper, collectionMapper, flashNoteMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteCollection("alice", 10L));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        verify(flashNoteMapper, never()).selectList(any());
    }
}
