# common-cache

## 项目概述

基于 Redisson 的统一缓存服务，提供分布式缓存、分布式锁、缓存队列等功能，使用静态方法调用，无需注入。

## 面向开发者

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.dev.lib</groupId>
    <artifactId>common-cache</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password
      database: 0
```

### 3. 使用方式

#### 3.1 单值缓存

```java
// 设置缓存
RedisCache.key("user:1").set(user);

// 获取缓存
User user = RedisCache.key("user:1").get().value();

// 设置过期时间
RedisCache.key("user:1").ttl(Duration.ofMinutes(30)).set(user);

// 获取并加载（缓存穿透保护）
User user = RedisCache.key("user:1").get().orElse(() -> loadFromDB(id));

// 删除缓存
RedisCache.key("user:1").delete();

// 检查是否存在
boolean exists = RedisCache.key("user:1").exists();

// 获取剩余 TTL
long ttl = RedisCache.key("user:1").getTtl();
```

#### 3.2 Map 缓存

```java
// 获取 Map
CacheMap map = RedisCache.key("map:key").map();

// 基本操作
map.put("field1", "value1");
Object value = map.get("field1");

// 条件操作
map.putIfAbsent("field1", "value1");
map.computeIfAbsent("field1", k -> loadFromDb(k));

// 批量操作
Map<String, Object> batch = new HashMap<>();
batch.put("field1", "value1");
batch.put("field2", "value2");
map.putAll(batch);

// 检查
boolean hasKey = map.containsKey("field1");
int size = map.size();

// 删除
map.remove("field1");
```

#### 3.3 List 缓存

```java
// 获取 List
CacheList<String> list = RedisCache.key("list:key").list();

// 添加
list.add("item1");
list.addAll(Arrays.asList("item2", "item3"));

// 获取
String item = list.get(0);
List<String> all = list.readAll();

// 删除
list.remove(0);
list.clear();
```

#### 3.4 Set 缓存

```java
// 获取 Set
CacheSet<String> set = RedisCache.key("set:key").set();

// 操作
set.add("item1");
set.addAll(Arrays.asList("item2", "item3"));

// 检查
boolean contains = set.contains("item1");
int size = set.size();

// 删除
set.remove("item1");
```

#### 3.5 阻塞队列

```java
// 获取阻塞队列
CacheBlockingQueue<String> queue = RedisCache.key("queue:key").blockingQueue();

// 生产者
queue.offer("task1");

// 消费者（阻塞）
String task = queue.take();

// 非阻塞
String task = queue.poll();
```

#### 3.6 原子计数器

```java
// 获取原子计数器
CacheAtomicLong counter = RedisCache.key("counter:key").atomicLong();

// 增减
long value = counter.incrementAndGet();
long value = counter.getAndAdd(10);

// 设置
counter.set(100);

// 获取
long value = counter.get();
```

#### 3.7 分布式锁

```java
// 获取锁
RLock lock = RedisCache.key("lock:resource").lock();

// 使用
try {
    lock.lock();
    // 临界区代码
    doSomething();
} finally {
    lock.unlock();
}

// 尝试加锁
if (lock.tryLock(10, TimeUnit.SECONDS)) {
    try {
        doSomething();
    } finally {
        lock.unlock();
    }
}

// 公平锁
RLock fairLock = RedisCache.key("lock:resource").fairLock();

// 读写锁
RReadWriteLock rwLock = RedisCache.key("lock:resource").readWriteLock();
rwLock.readLock().lock();
rwLock.writeLock().lock();
```

#### 3.8 布隆过滤器

```java
// 获取布隆过滤器
RBloomFilter<String> filter = RedisCache.key("bloom:user").bloomFilter();

// 初始化
filter.tryInit(100000, 0.01);

// 添加
filter.add("user1");

// 检查
boolean exists = filter.contains("user1");
```

#### 3.9 删除模式匹配

```java
// 删除匹配模式的所有 key
RedisCache.deletePattern("cache:user:*");
```

## 面向 LLM

### 核心组件

1. **RedisCache**：静态入口类
   - `key(...)`：创建缓存 key
   - `deletePattern(...)`：批量删除

2. **CacheKey**：缓存 key 操作
   - `.ttl()`：设置过期时间
   - `.get()`：获取单值
   - `.set()`：设置单值
   - `.map()`：获取 Map
   - `.list()`：获取 List
   - `.set()`：获取 Set
   - `.queue()`：获取队列
   - `.atomicLong()`：获取原子计数器
   - `.lock()`：获取锁

### 最近修改

暂无重大修改

### 修改记录位置

- 核心代码：`common-cache/src/main/java/com/dev/lib/cache/`
- Git 提交：使用 `git log --oneline common-cache/` 查看
