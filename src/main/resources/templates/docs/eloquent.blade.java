@extends('docs.layout')

@section('content')
<h1>Eloquent ORM</h1>
<p>Jaravel 的 Eloquent ORM 采用合并模式，单一类同时承担实体定义与查询能力，对齐 Laravel Eloquent，无需拆分 Entity 和 Model。</p>

<h2>Model 定义</h2>
<pre><code>@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "users")
public class User extends BaseModel&lt;User, Long&gt; implements Authenticatable {

    @Primary @Column(name = "id")   private Long id;
    @Column(name = "name")          private String name;
    @Column(name = "number")        private String number;
    @Column(name = "password")      private String password;
    @Column(name = "email")         private String email;

    // ---- 静态查询方法（委托给 BaseModel） ----
    public static User find(Long id) { return BaseModel.find(User.class, id); }
    public static List&lt;User&gt; all() { return BaseModel.all(User.class); }
    public static QueryBuilder&lt;User, Long&gt; query() { return BaseModel.query(User.class); }

    public static User findByNumber(String number) {
        Record&lt;User, Long&gt; record = query().where("number", number).first();
        return record == null ? null : record.toObject();
    }
}</code></pre>

<h2>CRUD 操作</h2>
<pre><code>// 创建
User user = new User();
user.setName("alice");
user.setNumber("alice001");
user.setPassword("secret123");
user.setEmail("alice@test.com");
user.save();

// 查询
User found = User.find(1L);
List&lt;User&gt; all = User.all();

// 更新
User user = User.find(1L);
user.setEmail("new@test.com");
user.save();

// 删除
User user = User.find(1L);
user.delete();</code></pre>

<h2>查询构造器</h2>
<pre><code>// 条件查询
User user = User.query()
    .where("number", "alice001")
    .first()
    .toObject();

// 多条件查询
List&lt;User&gt; users = User.query()
    .where("name", "alice")
    .where("status", 1)
    .orderBy("created_at", "desc")
    .limit(10)
    .get()
    .toObjectList();

// 聚合查询
long count = User.query().count();</code></pre>

<h2>多数据源</h2>
<p>通过 <code>@DataSource</code> 注解指定 Model 使用的数据源 Bean 名称，对齐 Laravel Model 的 <code>$connection</code> 属性：</p>
<pre><code>@Data
@Repository
@DataSource("secondaryGaarasonDataSource")  // 使用第二数据源
@Table(name = "products")
public class Product extends BaseModel&lt;Product, Long&gt; {
    @Primary @Column(name = "id")   private Long id;
    @Column(name = "name")          private String name;
    @Column(name = "price")         private Double price;

    public static Product find(Long id) { return BaseModel.find(Product.class, id); }
    public static List&lt;Product&gt; all()   { return BaseModel.all(Product.class); }
}</code></pre>

<h2>多数据源配置</h2>
<pre><code>@Configuration
public class Database {
    @Bean @Primary
    public GaarasonDataSource gaarasonDataSource(Environment env, ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.url", "jdbc:sqlite:database1.sqlite"));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }

    @Bean("secondaryGaarasonDataSource")
    public GaarasonDataSource secondaryGaarasonDataSource(Environment env, ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.secondary.url", "jdbc:sqlite:database3.sqlite"));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }
}</code></pre>

<h2>迁移</h2>
<p>迁移类名采用 <code>Migration_YYYY_MM_DD_PascalCaseDescription</code> 约定，按类名字典序排序保证执行顺序：</p>
<pre><code>@Component
public class Migration_2024_01_01_CreateUsersTable implements Migration {
    @Override
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name", 50);
            table.string("number", 50);
            table.string("password", 100);
            table.string("email", 50).nullable();
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("users");
    }
}</code></pre>

<div class="note">
    <strong>注意：</strong>BaseModel 的 ORM 核心基于 gaarason database-query 库。Model 类需标注 @Repository 注解以被 Spring 容器管理，@DataSource 注解指定数据源时需确保对应的 GaarasonDataSource Bean 已在 Database.java 中注册。
</div>
@endsection
