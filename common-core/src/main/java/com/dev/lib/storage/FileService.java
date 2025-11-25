package com.dev.lib.storage;

import com.dev.lib.storage.serialize.FileItem;

public interface FileService {

    FileItem getItem(String value);
}
