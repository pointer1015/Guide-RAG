package com.guiderag.knowledge.controller;

import com.github.pagehelper.PageInfo;
import com.guiderag.knowledge.model.dto.DocumentCreateReqDTO;
import com.guiderag.knowledge.model.dto.DocumentResDTO;
import com.guiderag.knowledge.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @Test
    void createDocument_shouldReturnSuccess() throws Exception {
        when(documentService.createDocument(eq(2001L), any(DocumentCreateReqDTO.class))).thenReturn(3001L);

        DocumentCreateReqDTO dto = new DocumentCreateReqDTO();
        dto.setFileName("a.pdf");
        dto.setFileType("pdf");
        dto.setFileSize(10L);
        dto.setMinioBucket("bucket");
        dto.setMinioObjectKey("path/a.pdf");

        mockMvc.perform(post("/rag/v1/knowledge-bases/2001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3001));
    }

    @Test
    void list_shouldReturnSuccess() throws Exception {
        DocumentResDTO dto = new DocumentResDTO();
        dto.setId(3001L);
        dto.setFileName("a.pdf");
        PageInfo<DocumentResDTO> pageInfo = new PageInfo<>();
        pageInfo.setList(List.of(dto));
        pageInfo.setTotal(1);
        pageInfo.setPageNum(1);
        pageInfo.setPageSize(10);
        pageInfo.setPages(1);
        when(documentService.list(2001L, 1, 10)).thenReturn(pageInfo);

        mockMvc.perform(get("/rag/v1/knowledge-bases/2001/documents")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(3001));
    }

    @Test
    void getById_shouldReturnSuccess() throws Exception {
        DocumentResDTO dto = new DocumentResDTO();
        dto.setId(3001L);
        dto.setKnowledgeBaseId(2001L);
        when(documentService.getById(2001L, 3001L)).thenReturn(dto);

        mockMvc.perform(get("/rag/v1/knowledge-bases/2001/documents/3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3001));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        doNothing().when(documentService).delete(2001L, 3001L);

        mockMvc.perform(delete("/rag/v1/knowledge-bases/2001/documents/3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

