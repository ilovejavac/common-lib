# common-excel

## 项目概述

基于 EasyExcel 的导入导出功能，通过注解简化 Excel 文件处理。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-excel</artifactId>
</dependency>
```

### 2. Excel 导入

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 方法上标注 @ExcelImport
    // 参数上标注 @ExcelData，类型为 List<你的实体类>
    @PostMapping("/import")
    @ExcelImport(fileParam = "file")  // 指定文件参数名
    public ServerResponse<Void> importUsers(@ExcelData List<UserImportDto> users) {
        
        // users 已自动解析为 List<UserImportDto>
        userService.batchImport(users);
        return ServerResponse.success();
    }
}

// 定义 DTO（使用 EasyExcel 注解）
@Data
public class UserImportDto {
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("邮箱")
    private String email;
    
    @ExcelProperty("年龄")
    private Integer age;
}

// 前端调用
// POST /api/users/import
// Content-Type: multipart/form-data
// file: users.xlsx
```

### 3. Excel 导出

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 方法上标注 @ExcelExport
    // 返回 List<你的实体类>
    @GetMapping("/export")
    @ExcelExport(
        fileName = "用户列表_${date}",  // 支持 ${date}, ${datetime}, ${timestamp}
        sheetName = "用户数据"
    )
    public List<UserExportDto> exportUsers(UserQuery query) {
        
        // 查询数据
        return userService.query(query);
    }
}

// 定义 DTO（使用 EasyExcel 注解）
@Data
public class UserExportDto {
    
    @ExcelProperty("用户ID")
    private Long id;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("邮箱")
    private String email;
    
    @ExcelProperty("状态")
    private String status;
    
    @ExcelProperty("创建时间")
    private LocalDateTime createdAt;
}

// 前端调用
// GET /api/users/export?status=ACTIVE
// 响应：application/octet-stream
// 文件名：用户列表_20250108.xlsx
```

### 4. 完整示例

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // 导入
    @PostMapping("/import")
    @ExcelImport
    public ServerResponse<ImportResult> importProducts(@ExcelData List<ProductImportDto> products) {
        
        ImportResult result = productService.importProducts(products);
        return ServerResponse.success(result);
    }
    
    // 导出
    @GetMapping("/export")
    @ExcelExport(fileName = "产品列表_${datetime}", sheetName = "产品")
    public List<ProductExportDto> exportProducts(ProductQuery query) {
        
        return productService.queryForExport(query);
    }
}

@Data
public class ProductImportDto {
    
    @ExcelProperty("产品名称")
    private String name;
    
    @ExcelProperty("产品编码")
    private String code;
    
    @ExcelProperty("价格")
    private BigDecimal price;
    
    @ExcelProperty("库存")
    private Integer stock;
}

@Data
public class ProductExportDto {
    
    @ExcelProperty("产品ID")
    private Long id;
    
    @ExcelProperty("产品名称")
    private String name;
    
    @ExcelProperty("产品编码")
    private String code;
    
    @ExcelProperty("价格")
    private BigDecimal price;
    
    @ExcelProperty("库存")
    private Integer stock;
    
    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

## 面向 LLM

### 核心组件

1. **@ExcelImport**：方法注解
   - `fileParam`：指定文件参数名（默认 "file"）

2. **@ExcelData**：参数注解
   - 标注方法参数，自动解析 Excel 为 List<实体类>
   - 必须配合 @ExcelImport 使用

3. **@ExcelExport**：方法注解
   - `fileName`：文件名模板（支持 ${date}, ${datetime}, ${timestamp}）
   - `sheetName`：Sheet 名称

4. **ExcelImportArgumentResolver**：参数解析器
   - 解析 MultipartFile 为 List<实体类>

5. **ExcelExportReturnValueHandler**：返回值处理器
   - 将 List<实体类> 转换为 Excel 文件下载

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-excel/src/main/java/com/dev/lib/excel/`
- Git 提交：使用 `git log --oneline common-excel/` 查看
