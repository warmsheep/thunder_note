package com.flashnote.flashnote;

import com.flashnote.auth.entity.User;
import com.flashnote.auth.mapper.UserMapper;
import com.flashnote.common.exception.BusinessException;
import com.flashnote.common.response.ErrorCode;
import com.flashnote.flashnote.entity.FlashNote;
import com.flashnote.flashnote.mapper.FlashNoteMapper;
import com.flashnote.flashnote.service.impl.FlashNoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashNoteServiceImplTest {

    private UserMapper createMockUserMapper(Long userId, String username) {
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        when(userMapper.selectOne(any())).thenReturn(user);
        return userMapper;
    }

    @Test
    void listNotes_returnsUserNotes() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        FlashNote note1 = new FlashNote();
        note1.setId(1L);
        note1.setUserId(1L);
        note1.setTitle("Note 1");
        note1.setContent("Content 1");
        note1.setDeleted(false);

        FlashNote note2 = new FlashNote();
        note2.setId(2L);
        note2.setUserId(1L);
        note2.setTitle("Note 2");
        note2.setContent("Content 2");
        note2.setDeleted(false);

        when(flashNoteMapper.selectList(any())).thenReturn(List.of(note1, note2));

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When
        List<FlashNote> result = service.listNotes("alice");

        // Then
        assertEquals(2, result.size());
        assertEquals("Note 1", result.get(0).getTitle());
        assertEquals("Note 2", result.get(1).getTitle());
        verify(flashNoteMapper).selectList(any());
    }

    @Test
    void listNotes_returnsEmptyListWhenNoNotes() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);
        when(flashNoteMapper.selectList(any())).thenReturn(Collections.emptyList());

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When
        List<FlashNote> result = service.listNotes("alice");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void createNote_withValidData_createsNote() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        FlashNote inputNote = new FlashNote();
        inputNote.setTitle("New Note");
        inputNote.setIcon("💡");
        inputNote.setContent("New Content");
        inputNote.setTags("tag1,tag2");

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When
        FlashNote result = service.createNote("alice", inputNote);

        // Then
        assertEquals(1L, result.getUserId());
        assertEquals("New Note", result.getTitle());
        assertEquals("💡", result.getIcon());
        assertEquals("New Content", result.getContent());
        assertEquals("tag1,tag2", result.getTags());
        assertFalse(result.getDeleted());
        verify(flashNoteMapper).insert(result);
    }

    @Test
    void updateNote_withValidData_updatesNote() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        FlashNote existingNote = new FlashNote();
        existingNote.setId(1L);
        existingNote.setUserId(1L);
        existingNote.setTitle("Old Title");
        existingNote.setIcon("📝");
        existingNote.setContent("Old Content");
        existingNote.setTags("old");
        existingNote.setDeleted(false);

        when(flashNoteMapper.selectOne(any())).thenReturn(existingNote);

        FlashNote updateData = new FlashNote();
        updateData.setTitle("Updated Title");
        updateData.setIcon("📚");
        updateData.setContent("Updated Content");
        updateData.setTags("updated");

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When
        FlashNote result = service.updateNote("alice", 1L, updateData);

        // Then
        assertEquals("Updated Title", result.getTitle());
        assertEquals("📚", result.getIcon());
        assertEquals("Updated Content", result.getContent());
        assertEquals("updated", result.getTags());
        verify(flashNoteMapper).updateById(existingNote);
    }

    @Test
    void updateNote_withWrongUser_throwsException() {
        // Given
        UserMapper userMapper = mock(UserMapper.class);
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userMapper.selectOne(any())).thenReturn(alice, bob);

        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        // Alice's note in DB
        FlashNote aliceNote = new FlashNote();
        aliceNote.setId(1L);
        aliceNote.setUserId(1L);
        aliceNote.setTitle("Alice's Note");
        aliceNote.setDeleted(false);

        // Bob tries to update Alice's note - query returns null because userId doesn't match
        when(flashNoteMapper.selectOne(any())).thenReturn(null);

        FlashNote updateData = new FlashNote();
        updateData.setTitle("Hacked");

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.updateNote("bob", 1L, updateData));
        
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("Note not found", exception.getMessage());
    }

    @Test
    void deleteNote_withValidId_deletesNote() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        FlashNote existingNote = new FlashNote();
        existingNote.setId(1L);
        existingNote.setUserId(1L);
        existingNote.setTitle("To Delete");
        existingNote.setDeleted(false);

        when(flashNoteMapper.selectOne(any())).thenReturn(existingNote);

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When
        service.deleteNote("alice", 1L);

        // Then
        assertTrue(existingNote.getDeleted());
        verify(flashNoteMapper).updateById(existingNote);
    }

    @Test
    void deleteNote_withNonExistentId_throwsException() {
        // Given
        UserMapper userMapper = createMockUserMapper(1L, "alice");
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);
        when(flashNoteMapper.selectOne(any())).thenReturn(null);

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.deleteNote("alice", 999L));
        
        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        assertEquals("Note not found", exception.getMessage());
    }

    @Test
    void listNotes_withInvalidUser_throwsException() {
        // Given
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(null);
        FlashNoteMapper flashNoteMapper = mock(FlashNoteMapper.class);

        FlashNoteServiceImpl service = new FlashNoteServiceImpl(userMapper, flashNoteMapper);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> service.listNotes("nonexistent"));
        
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), exception.getCode());
    }
}
