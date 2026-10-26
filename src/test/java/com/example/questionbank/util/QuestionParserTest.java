package com.example.questionbank.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析器回归测试，覆盖真实题库里遇到的各种排版问题。
 */
class QuestionParserTest {

    @Test
    void 解析标准单选题与判断题() {
        String text = """
                技能提升题库-题库
                1、浏览网页,是属于 Internet 所提供的( )服务网页
                A、FTP
                B、Email
                C、Telnet
                D、WWW
                答案：D
                2、打开网站时,看到的第一个网页一般称为首页或主页。
                答案：正确
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(2, questions.size());
        assertEquals("single", questions.get(0).getQuestionType());
        assertEquals("D", questions.get(0).getCorrectAnswer());
        assertEquals("WWW", questions.get(0).getOptionD());
        assertEquals("judge", questions.get(1).getQuestionType());
        assertEquals("正确", questions.get(1).getCorrectAnswer());
    }

    @Test
    void 选项换行与题干续行会被合并() {
        String text = """
                1、下列哪一项的说法是正确的,请注意这道题干的文字比较长会被排成两
                行显示
                A、开发技术不同,静态网页主要采用HTML语言,而动态网页可采用
                HTML+PHP等
                B、第二个选项
                答案：A
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(1, questions.size());
        String content = questions.get(0).getContent();
        assertTrue(content.contains("两行显示"), "题干续行应被合并: " + content);
        assertTrue(questions.get(0).getOptionA().contains("HTML+PHP等"), "选项续行应被合并");
    }

    @Test
    void 一行多个选项() {
        String text = """
                1、下列哪些属于块元素
                A、div B、span C、p D、ul
                答案：ACD
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(1, questions.size());
        assertEquals(4, questions.get(0).getOptions().size());
        assertEquals("div", questions.get(0).getOptionA());
        assertEquals("multiple", questions.get(0).getQuestionType());
    }

    @Test
    void 题号丢失时仍按答案行切题() {
        String text = """
                1、第一题
                A、甲
                B、乙
                答案：A
                第二题没有题号
                A、甲
                B、乙
                答案：B
                2、第三题
                A、甲
                B、乙
                答案：A
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(3, questions.size());
        assertEquals("第二题没有题号", questions.get(1).getContent());
        assertEquals("B", questions.get(1).getCorrectAnswer());
    }

    @Test
    void 题号被排到行尾() {
        String text = """
                1、第一题
                答案：正确
                在网页中显示图像的HTML标签是 2、
                A、image
                B、img
                答案：B
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(2, questions.size());
        assertEquals(2, questions.get(1).getNumber());
        assertEquals("在网页中显示图像的HTML标签是", questions.get(1).getContent());
        assertEquals("B", questions.get(1).getCorrectAnswer());
    }

    @Test
    void 上一题没有解析时不吞并下一题() {
        String text = """
                1、第一题
                答案：A
                2、第二题
                A、甲
                B、乙
                答案：B
                解析：因为乙更合适
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(2, questions.size());
        assertEquals("第二题", questions.get(1).getContent());
        assertEquals("因为乙更合适", questions.get(1).getAnalysis());
    }

    @Test
    void 多空填空题答案跨行拼接() {
        String text = """
                1、在填空处填出代号
                ______第[1]空_______
                答案：
                a;b
                ；c
                答案：a;b；c
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(1, questions.size());
        assertEquals("fill", questions.get(0).getQuestionType());
        assertNotNull(questions.get(0).getCorrectAnswer());
    }

    @Test
    void 页面页码与页眉被过滤() {
        String text = """
                技能提升题库-题库
                - 12 -
                1、第一题
                答案：正确
                技能提升题库-题库
                第 13 页
                2、第二题
                答案：错误
                """;
        List<QuestionData> questions = QuestionParser.parse(text);
        assertEquals(2, questions.size());
    }
}
