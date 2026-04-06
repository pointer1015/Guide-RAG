package com.guiderag.knowledge.service.impl;

import com.github.pagehelper.PageInfo;
import com.guiderag.common.context.UserContextHolder;
import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.mapper.DocumentMapper;
import com.guiderag.knowledge.mapper.KnowledgeBaseMapper;
import com.guiderag.knowledge.model.dto.DocumentCreateReqDTO;
import com.guiderag.knowledge.model.dto.DocumentResDTO;
import com.guiderag.knowledge.model.entity.Document;
import com.guiderag.knowledge.model.entity.KnowledgeBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private static final Long USER_ID = 1001L;
    private static final Long KB_ID = 2001L;
    private static final Long OTHER_KB_ID = 2002L;
    private static final Long DOC_ID = 3001L;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void createDocument_shouldThrow_whenKnowledgeBaseNotFound() {
        when(knowledgeBaseMapper.selectById(KB_ID, USER_ID)).thenReturn(null);

        DocumentCreateReqDTO dto = new DocumentCreateReqDTO();
        dto.setFileName("a.pdf");
        dto.setFileType("pdf");
        dto.setFileSize(10L);
        dto.setMinioBucket("bucket");
        dto.setMinioObjectKey("path/a.pdf");

        BizException ex = assertThrows(BizException.class, () -> documentService.createDocument(KB_ID, dto));
        assertEquals("A0500", ex.getCode());
        verify(documentMapper, never()).insert(any());
    }

    @Test
    void createDocument_shouldThrow_whenHashDuplicated() {
        when(knowledgeBaseMapper.selectById(KB_ID, USER_ID)).thenReturn(new KnowledgeBase());
        when(documentMapper.countByKbAndHash(KB_ID, USER_ID, "hash1")).thenReturn(1L);

        DocumentCreateReqDTO dto = new DocumentCreateReqDTO();
        dto.setFileName("a.pdf");
        dto.setFileType("pdf");
        dto.setFileSize(10L);
        dto.setMinioBucket("bucket");
        dto.setMinioObjectKey("path/a.pdf");
        dto.setContentHash("hash1");

        BizException ex = assertThrows(BizException.class, () -> documentService.createDocument(KB_ID, dto));
        assertEquals("A0400", ex.getCode());
        verify(documentMapper, never()).insert(any());
    }

    @Test
    void createDocument_shouldInsertPendingDocument_whenValid() {
        when(knowledgeBaseMapper.selectById(KB_ID, USER_ID)).thenReturn(new KnowledgeBase());
        when(documentMapper.countByKbAndHash(KB_ID, USER_ID, "hash1")).thenReturn(0L);

        DocumentCreateReqDTO dto = new DocumentCreateReqDTO();
        dto.setTitle("doc");
        dto.setFileName("a.pdf");
        dto.setFileType("pdf");
        dto.setFileSize(10L);
        dto.setMimeType("application/pdf");
        dto.setMinioBucket("bucket");
        dto.setMinioObjectKey("path/a.pdf");
        dto.setContentHash("hash1");

        Long result = documentService.createDocument(KB_ID, dto);
        assertNotNull(result);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentMapper).insert(captor.capture());
        Document inserted = captor.getValue();
        assertEquals(USER_ID, inserted.getTenantId());
        assertEquals(USER_ID, inserted.getUploadedBy());
        assertEquals(KB_ID, inserted.getKnowledgeBaseId());
        assertEquals("PENDING", inserted.getParseStatus());
    }

    @Test
    void list_shouldThrow_whenNotLoggedIn() {
        UserContextHolder.clear();
        BizException ex = assertThrows(BizException.class, () -> documentService.list(KB_ID, 1, 10));
        assertEquals("A0200", ex.getCode());
    }

    @Test
    void list_shouldThrow_whenKnowledgeBaseNotFound() {
        when(knowledgeBaseMapper.selectById(KB_ID, USER_ID)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> documentService.list(KB_ID, 1, 10));
        assertEquals("A0500", ex.getCode());
    }

    @Test
    void list_shouldReturnDtoPage_whenValid() {
        when(knowledgeBaseMapper.selectById(KB_ID, USER_ID)).thenReturn(new KnowledgeBase());
        Document doc = new Document();
        doc.setId(DOC_ID);
        doc.setKnowledgeBaseId(KB_ID);
        doc.setFileName("a.pdf");
        doc.setFileType("pdf");
        doc.setFileSize(10L);
        doc.setParseStatus("PENDING");
        when(documentMapper.selectByKnowledgeBaseId(KB_ID, USER_ID)).thenReturn(List.of(doc));

        PageInfo<DocumentResDTO> page = documentService.list(KB_ID, 1, 10);
        assertEquals(1, page.getList().size());
        assertEquals(DOC_ID, page.getList().get(0).getId());
        verify(documentMapper).selectByKnowledgeBaseId(KB_ID, USER_ID);
    }

    @Test
    void getById_shouldThrow_whenDocumentNotFoundOrForbidden() {
        when(documentMapper.selectById(DOC_ID, KB_ID, USER_ID)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> documentService.getById(KB_ID, DOC_ID));
        assertEquals("A0500", ex.getCode());
    }

    @Test
    void getById_shouldThrow_whenCrossKnowledgeBaseAccess() {
        // docId 实际属于 OTHER_KB_ID，请求却使用 KB_ID，期望被拦截（查询结果为空）
        when(documentMapper.selectById(DOC_ID, KB_ID, USER_ID)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> documentService.getById(KB_ID, DOC_ID));
        assertEquals("A0500", ex.getCode());
        verify(documentMapper).selectById(DOC_ID, KB_ID, USER_ID);
    }

    @Test
    void getById_shouldReturnDto_whenFound() {
        Document doc = new Document();
        doc.setId(DOC_ID);
        doc.setKnowledgeBaseId(KB_ID);
        doc.setFileName("a.pdf");
        doc.setParseStatus("PARSED");
        when(documentMapper.selectById(DOC_ID, KB_ID, USER_ID)).thenReturn(doc);

        DocumentResDTO dto = documentService.getById(KB_ID, DOC_ID);
        assertEquals(DOC_ID, dto.getId());
        assertEquals(KB_ID, dto.getKnowledgeBaseId());
    }

    @Test
    void delete_shouldThrow_whenDocumentNotFoundOrForbidden() {
        when(documentMapper.selectById(DOC_ID, KB_ID, USER_ID)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> documentService.delete(KB_ID, DOC_ID));
        assertEquals("A0500", ex.getCode());
        verify(documentMapper, never()).deleteById(any(), any(), any());
    }

    @Test
    void delete_shouldSoftDelete_whenFound() {
        when(documentMapper.selectById(DOC_ID, KB_ID, USER_ID)).thenReturn(new Document());
        when(documentMapper.deleteById(DOC_ID, KB_ID, USER_ID)).thenReturn(1);

        assertDoesNotThrow(() -> documentService.delete(KB_ID, DOC_ID));
        verify(documentMapper).deleteById(eq(DOC_ID), eq(KB_ID), eq(USER_ID));
    }
}

