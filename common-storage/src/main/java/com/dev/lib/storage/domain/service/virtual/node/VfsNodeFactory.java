package com.dev.lib.storage.domain.service.virtual.node;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * VFS 节点工厂
 * 负责将 SysFile 实体转换为 VfsNode DTO
 */
@Component
@RequiredArgsConstructor
public class VfsNodeFactory {

    private final VfsFileRepository fileRepository;

    private final VfsPathResolver   pathResolver;

    /**
     * 批量构建节点列表
     *
     * @param ctx   VFS 上下文
     * @param files 文件实体列表
     * @param depth 递归深度
     * @return 节点列表
     */
    public List<VfsNode> buildNodes(VfsContext ctx, List<SysFile> files, int depth) {

        List<VfsNode> nodes = new ArrayList<>();
        for (SysFile file : files) {
            if (Boolean.TRUE.equals(file.getHidden()) && !ctx.isShowHidden()) {
                continue;
            }
            nodes.add(toNode(ctx, file, depth - 1));
        }
        return nodes;
    }

    /**
     * 将单个文件实体转换为节点
     *
     * @param ctx            VFS 上下文
     * @param file           文件实体
     * @param remainingDepth 剩余递归深度
     * @return 节点
     */
    public VfsNode toNode(VfsContext ctx, SysFile file, int remainingDepth) {

        VfsNode node = new VfsNode();
        node.setId(file.getBizId());
        node.setName(pathResolver.getName(file.getVirtualPath()));
        node.setPath(file.getVirtualPath());
        node.setIsDirectory(file.getIsDirectory());
        node.setSize(file.getSize());
        node.setExtension(file.getExtension());
        node.setModifiedAt(file.getUpdatedAt());

        if (Boolean.TRUE.equals(file.getIsDirectory()) && remainingDepth > 0) {
            List<SysFile> children = fileRepository.findChildren(file.getVirtualPath());
            node.setChildren(buildNodes(ctx, children, remainingDepth));
        }
        return node;
    }

}
