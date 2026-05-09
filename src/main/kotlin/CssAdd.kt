package com.example

object CssAdd {
    fun getCss(): String = """
    body {
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        background-color: #f4f7f6;
        color: #333;
        line-height: 1.6;
        display: flex;
        justify-content: center;
        padding: 40px;
    }
    .container {
        background: #fff;
        padding: 30px;
        border-radius: 8px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        max-width: 600px;
        width: 100%;
    }
    h1, h2, h3 { color: #2c3e50; }
    a { color: #3498db; text-decoration: none; }
    a:hover { text-decoration: underline; }
    .btn {
        background: #3498db;
        color: #fff;
        padding: 10px 20px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
    }
    .btn:hover { background: #2980b9; }
    .form-group { margin-bottom: 20px; }
    textarea, input[type='text'], input[type='password'], select {
        width: 100%;
        padding: 10px;
        border: 1px solid #ddd;
        border-radius: 4px;
        box-sizing: border-box;
    }
    ul { list-style: none; padding: 0; }
    li { background: #eee; margin-bottom: 5px; padding: 10px; border-radius: 4px; }
""".trimIndent()
}