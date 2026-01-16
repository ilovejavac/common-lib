package com.dev.lib.storage.domain.service.virtual.search;

/**
 * VFS Glob 模式匹配器
 * 提供简单的通配符匹配功能（支持 * 和 ?）
 */
public final class VfsGlobMatcher {

    private VfsGlobMatcher() {
        // 工具类，不允许实例化
    }

    /**
     * 匹配 glob 模式
     *
     * @param glob 通配符模式（支持 * 和 ?）
     * @param name 要匹配的名称
     * @return 是否匹配
     */
    public static boolean matches(String glob, String name) {

        if (glob == null || glob.isEmpty()) {
            return true;
        }
        if (name == null) {
            return false;
        }

        // 优化：如果没有通配符，直接比较
        if (glob.indexOf('*') < 0 && glob.indexOf('?') < 0) {
            return glob.equals(name);
        }

        // 使用简化的 glob 匹配算法
        return match(glob, 0, name, 0);
    }

    /**
     * 递归匹配 glob 模式
     * 使用回溯算法处理 * 通配符
     */
    private static boolean match(String glob, int gi, String name, int ni) {

        int glen = glob.length();
        int nlen = name.length();

        while (gi < glen && ni < nlen) {
            char gc = glob.charAt(gi);
            char nc = name.charAt(ni);

            if (gc == '?') {
                // ? 匹配任意单个字符
                gi++;
                ni++;
            } else if (gc == '*') {
                // * 匹配 0 或多个字符
                // 跳过连续的 *
                while (gi < glen && glob.charAt(gi) == '*') {
                    gi++;
                }
                // 如果 * 后面没有其他字符，匹配成功
                if (gi == glen) {
                    return true;
                }
                // 尝试匹配 * 后面的模式
                return matchStar(glob, gi, name, ni);
            } else if (gc != nc) {
                return false;
            } else {
                gi++;
                ni++;
            }
        }

        // 处理末尾的 *
        while (gi < glen && glob.charAt(gi) == '*') {
            gi++;
        }

        return gi == glen && ni == nlen;
    }

    /**
     * 匹配 * 通配符后的模式
     * 使用回溯算法尝试所有可能的匹配位置
     */
    private static boolean matchStar(String glob, int gi, String name, int ni) {

        int nlen = name.length();

        // 尝试从当前位置开始的每个位置
        while (ni <= nlen) {
            if (match(glob, gi, name, ni)) {
                return true;
            }
            ni++;
        }

        return false;
    }

}
