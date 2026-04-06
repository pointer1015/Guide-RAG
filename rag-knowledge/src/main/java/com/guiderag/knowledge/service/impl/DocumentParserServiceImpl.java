package com.guiderag.knowledge.service.impl;

import com.guiderag.common.exception.BizException;
import com.guiderag.knowledge.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析服务实现类
 *
 * 核心原理：
 * - AutoDetectParser：自动选择合适的解析器
 * - BodyContentHandler：提取纯文本内容（去除 HTML/XML 标签）
 * - Metadata：提取文档元数据（作者、创建时间、页数等）
 *
 */
@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    /**
     * 最大文本长度限制
     *
     * 原因：
     * - Tika 默认限制 100,000 字符（防止 OOM）
     * - 超长文本会被截断，抛出 WriteOutContentHandler.WriteLimitReachedException
     */
    private static final int MAX_TEXT_LENGTH = -1;  // -1 表示不限制


    @Override
    public String parseDocument(InputStream inputStream, String fileName) {
        log.info("[DocumentParser] 开始解析文档: fileName={}", fileName);

        try {
            // 1 创建tika自动检测解析器
            AutoDetectParser parser = new AutoDetectParser();

            // 2 创建内容处理器（用于提取纯文本）
            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);

            // 3 创建元数据容器（用于）
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);  // 设置文件名，帮助 Tika 识别类型

            // 4 执行解析
            parser.parse(inputStream, handler, metadata);

            // 5 获取解析结果
            String content = handler.toString();

            // 6 文本清洗
            String cleanedContent = cleanText(content);

            // 7 日志记录
            log.info("[DocumentParser] 解析完成: fileName={}, contentLength={}, mimeType={}",
                    fileName, cleanedContent.length(), metadata.get(Metadata.CONTENT_TYPE));

            // 8 打印元数据（调试用）
            logMetadata(metadata, fileName);

            return cleanedContent;
        }catch (TikaException e) {
            // Tika 解析异常
            log.error("[DocumentParser] Tika 解析失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BizException("B0001","文档解析失败，文件可能已损坏或格式不支持");

        } catch (IOException e) {
            // IO 异常（文件读取失败）
            log.error("[DocumentParser] 文件读取失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BizException("B0001","文件读取失败，请重新上传");

        } catch (SAXException e) {
            // XML 解析异常（HTML/XML 文件格式错误）
            log.error("[DocumentParser] SAX 解析失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BizException("B0001","文档格式错误，无法解析");

        } catch (Exception e) {
            log.error("[DocumentParser] 未知错误: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new BizException("B0001","文档解析失败: " + e.getMessage());
        }

    }


    /**
     * 文本清洗
     *
     * 清洗规则：
     * 1. 删除控制字符（\u0000-\u001F，不包括 \n \r \t）
     * 2. 删除零宽字符（\u200B \uFEFF）
     * 3. 合并多个连续空行为一个
     * 4. 删除首尾空白
     *
     * 为什么需要清洗？
     * - PDF 提取的文本可能包含控制字符
     * - Word 文档可能包含零宽字符
     * - 减少 Token 数量（节省 Embedding 成本）
     * - 提高文本质量（避免干扰语义）
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                // 删除控制字符（保留换行、回车、制表符）
                .replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "")
                // 删除零宽字符
                .replaceAll("[\\u200B\\uFEFF]", "")
                // 合并多个连续空行为一个
                .replaceAll("\\n{3,}", "\n\n")
                // 删除首尾空白
                .trim();
    }

    /**
     * 打印文档元数据（调试用）
     *
     * 常见元数据：
     * - Content-Type: 文件 MIME 类型（application/pdf, text/plain 等）
     * - Author: 作者
     * - Creation-Date: 创建时间
     * - Last-Modified: 最后修改时间
     * - Page-Count: 页数（PDF）
     * - Word-Count: 字数（Word）
     * - Application-Name: 创建软件（Microsoft Word, Adobe Acrobat 等）
     *
     * @param metadata 元数据对象
     * @param fileName 文件名
     */
    private void logMetadata(Metadata metadata, String fileName) {
        log.debug("[DocumentParser] 文档元数据: fileName={}", fileName);
        for (String name : metadata.names()) {
            log.debug("  - {}: {}", name, metadata.get(name));
        }
    }

}
