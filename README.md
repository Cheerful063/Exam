# 单人刷题软件（Question Bank）

从 PDF / Word / 图片题库文件自动解析题目，支持单选、多选、判断、填空、简答五种题型，边刷边记录进度。

> 解决的核心问题：很多题库 PDF 的题号会丢失、被排版到行尾，题干/选项/答案混在一起。
> 本项目以「答案」行为锚点切题，不依赖题号是否完整，实测 178 页题库能解析出 705 道有效题目。

## 功能

- **文件导入**：PDF、Word(.docx)、TXT/MD、图片（OCR）
- **智能解析**：自动识别题型、拆分同行多选项、合并换行续写的题干与选项、过滤页眉页码
- **五种题型**：单选 / 多选 / 判断 / 填空 / 简答，各自独立的判题规则
- **刷题界面**：题号导航、已答标记、对错反馈、解析展示、进度持久化
- **答题记录**：每题保留最近一次作答，支持重答
- **手动录入**：文字或图片题目，可补充 A–D 选项
- **导入报告**：解析完成后返回题型分布 + 需要人工检查的题目提示
- **覆盖导入**：一键清空后重新导入，适合题库重复或题型混乱时重来

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 4.0.6 |
| 持久层 | MyBatis-Plus 3.5.15 + MySQL 8 |
| 文档解析 | PDFBox 3.0.2、Apache POI 5.2.5、Tesseract(可选 OCR) |
| 前端 | 原生 HTML/CSS/JS（无构建步骤） |
| JDK | 21 |

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+

### 2. 建库建表

```bash
mysql -uroot -p -e "CREATE DATABASE example_db DEFAULT CHARSET utf8mb4;"
mysql -uroot -p example_db < sql/schema.sql
```

### 3. 配置数据库密码

数据库信息不写死在代码里。复制模板并填入自己的密码：

```bash
cp application-local.yml.example application-local.yml
```

编辑 `application-local.yml`：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

> `application-local.yml` 已在 `.gitignore` 中，不会被提交。
> 也可以改用环境变量：`DB_USER` / `DB_PASSWORD` / `DB_HOST` / `DB_PORT` / `DB_NAME`。

### 4. 启动

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/question-bank-app-1.0.0.jar
```

打开 <http://localhost:8080>。

## 使用流程

1. 首页 → **上传题库**
2. 选择 PDF / Word / TXT / 图片文件
3. 点 **仅试解析**，先看能解析出多少题、有没有异常提示（不写库）
4. 确认无误后点 **开始上传**
5. 回首页 → **开始刷题**

## 配置项

| 配置 | 环境变量 | 默认值 |
|---|---|---|
| 服务端口 | `SERVER_PORT` | 8080 |
| 数据库地址 | `DB_HOST` / `DB_PORT` / `DB_NAME` | localhost / 3306 / example_db |
| 数据库账号 | `DB_USER` | root |
| 数据库密码 | `DB_PASSWORD` | 空 |
| 上传目录 | `file.upload-dir` | ./uploads/ |
| 单文件上限 | `spring.servlet.multipart.max-file-size` | 50MB |

## 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/questions` | 全部题目 |
| GET | `/api/questions/{id}` | 单题详情 |
| GET | `/api/questions/count` | 题目总数 |
| DELETE | `/api/questions/clear` | 清空题目、题库、答题记录 |
| POST | `/api/upload` | 上传并导入（`replace=true` 覆盖导入） |
| POST | `/api/upload/preview` | 仅解析试跑，不写库 |
| POST | `/api/upload/manual` | 手动添加题目（可带图片） |
| POST | `/api/upload/manual-text` | 手动添加纯文字题目 |
| POST | `/api/upload/image-question` | 上传图片题目 |
| POST | `/api/answers/submit` | 提交答案，返回对错与解析 |
| GET | `/api/answers/progress` | 答题进度 |
| DELETE | `/api/answers/clear` | 清除进度 |

## 解析器的切题规则

`QuestionParser` 是一个纯 JDK 状态机，与文件格式无关：

- **以答案行为锚点**：行号丢失也能切题，题号只用来显示
- **支持三种题号写法的混排**：`1、` `1．` `1.`，以及被排版到行尾的 `…… 664、`
- **同行多选项**：`A、甲 B、乙 C、丙` 会拆成三个选项
- **续行合并**：题干、选项、解析被换行拆开时自动接回去（中文直接拼，英文/数字补空格）
- **题型推断**：有选项且答案为多个字母 → 多选；选项只有「正确/错误」→ 判断题；题干含 `____` 或 `（）` → 填空题；其余 → 简答题
- **噪声过滤**：页码、`第 x 页 共 y 页`、页眉标题、纯标点行

### 判题规则

| 题型 | 规则 |
|---|---|
| 单选 | 忽略大小写与空格 |
| 多选 | 按字母集合比较，`ADC` 等于 `ACD`，`A,B,D` 也可识别 |
| 判断 | `对/正确/√/T/是/1` 等价，`错/×/F/否/0` 等价 |
| 填空/简答 | 忽略标点空格、互相包含、编辑距离相似度 ≥ 0.75 均判对 |

题库里标准答案为空时（源文件本身缺失），任何作答都算完成，避免出现永远答不对的题。

## 已知限制

- **扫描版 PDF** 需要额外安装 [Tesseract](https://github.com/UB-Mannheim/tesseract/wiki) 及中文语言包 `chi_sim`，否则会提示无法识别；最多识别前 60 页
- **PDF 双栏排版** 中，如果某行代码块与选项在垂直方向交错，文本抽取顺序可能串行（实测 705 题中有 1 例）
- 无用户体系，是本机单人使用的工具，**不要直接暴露到公网**
- 题目去重按「题干去掉标点空白后」比对，题干相同但选项不同的两份题目会被视为同一题

## 目录结构

```
src/main/java/com/example/questionbank/
├── config/          # 建表补列、CORS、静态资源映射、全局异常
├── controller/      # 题目、答题、上传、页面路由
├── entity/          # Question / QuestionBank / AnswerRecord
├── mapper/          # MyBatis-Plus Mapper
├── service/         # 业务接口与实现
└── util/            # 文件解析、题目解析器、DTO
src/main/resources/
├── application.yml  # 配置（敏感项走环境变量）
└── static/          # 三个页面 + 原生 JS
src/test/java/.../QuestionParserTest.java   # 解析器回归测试
sql/schema.sql                                 # 建表脚本
```

## 测试

```bash
mvn test
```

`QuestionParserTest` 覆盖了真实题库里遇到的各种排版问题：题号丢失、题号在行尾、一行多选项、选项换行、判断/多选答案归一化等。
