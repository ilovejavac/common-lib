package com.dev.lib.ai.service

import com.dev.lib.storage.domain.service.FileService
import org.springframework.stereotype.Component

/**
 * 知识库服务
 */
@Component
class DocumentService (
    val fileService: FileService
) {
}