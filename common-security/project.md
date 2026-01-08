# common-security

## 项目概述

安全认证授权基础框架，提供加密解密、权限模型等功能。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
app:
  security:
    encrypt-version: aes  # aes, base64, rsa, custom
```

### 3. 字段加密

```java
@Entity
public class User {
    
    @Encrypt  // 自动加密存储
    private String idCard;
    
    @Encrypt  // 自动加密存储
    private String phone;
    
    private String email;  // 不加密
}

// 查询时自动解密
User user = userRepository.findById(1L);
System.out.println(user.getIdCard());  // 自动解密后的明文
```

### 4. 自定义加密

```java
@Component
public class CustomEncryptor implements EncryptVersion.CustomEncryptor {
    
    @Override
    public String encrypt(String plaintext) {
        // 自定义加密逻辑
        return customEncrypt(plaintext);
    }
    
    @Override
    public String decrypt(String ciphertext) {
        // 自定义解密逻辑
        return customDecrypt(ciphertext);
    }
}

// 配置使用
app:
  security:
    encrypt-version: custom
```

## 面向 LLM

### 核心组件

1. **EncryptionService**：统一加密接口
   - encrypt(plaintext): 加密
   - decrypt(ciphertext): 解密

2. **EncryptVersion**：加密版本枚举
   - AES: AES 加密
   - BASE64: Base64 编码
   - RSA: RSA 加密
   - CUSTOM: 自定义加密

3. **@Encrypt**：字段加密注解
   - 自动加密存储
   - 自动解密读取

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-security/src/main/java/com/dev/lib/security/`
- Git 提交：使用 `git log --oneline common-security/` 查看
