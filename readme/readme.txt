说明：
    1. 提供GIF图片，编码方式都是89a
    2. 将要解析的gif图片放到 res/raw/ 目录下 ，名称必须是demo.gif， 程序会自动拷贝到应用程序的私有目录中 ，Android文件读取权限更加严格。
    3. 提供Java层解析和C层解析（giflib）
    4. 目的是对比Java和C 的处理消耗与时间

    结论： 实际发现差不了太多， 估计是Movie类做得比比较好


将Demo 提交github
