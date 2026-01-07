package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;

import java.io.InputStream;
import java.util.List;

public interface VirtualFileSystem {

    List<VfsNode> ls(VfsContext ctx, String path, Integer depth);

    InputStream cat(VfsContext ctx, String path);

    String read(VfsContext ctx, String path);

    void write(VfsContext ctx, String path, String content);

    void write(VfsContext ctx, String path, byte[] content);

    void mv(VfsContext ctx, String srcPath, String destPath);

    void cp(VfsContext ctx, String srcPath, String destPath);

    void rm(VfsContext ctx, String path);

    void rmrf(VfsContext ctx, String path);

    void mkdir(VfsContext ctx, String path);

    void mkdirp(VfsContext ctx, String path);

    void uploadZip(VfsContext ctx, String path, InputStream zipStream);

    boolean exists(VfsContext ctx, String path);

    boolean isDir(VfsContext ctx, String path);

}
