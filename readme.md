## Jane: A super lightweight static blog system

Jane 是一个超级轻量的静态博客系统，其工程文件、资源文件以及生成的 HTML 文档都极为精简，同时保持了页面的简洁和美观。且提供完整、方便的开发工具，修改文件后可立即自动更新页面，使用者可以专心于文档写作本身。

Jane 生成的示例博客如下：

* [Jane 主页](https://jane.pandolia.net)

* [Jane 作者的博客](https://pandolia.net)

#### 安装

安装 JRE/JDK 8+ ，下载 [jane.1.0.0.zip](https://jane.pandolia.net/download/jane.1.0.0.zip)，解压后，以管理员权限运行 register-this-path.bat 将解压后的目录添加至系统路径（或手工添加），之后打开一个终端，运行 jane 命令，若打印出使用信息，则表明安装成功。

#### 新建工程

运行 ***jane create your-project-name*** ，将创建一个 jane 工程，新建一个 your-project-name 目录，内包含 src 和 build 目录。其中： 

* src 目录下为源码文件，含文章（src/page）、模板（src/template.mustache）、资源（src/static） 和 配置（site.config）

* build 目录下是编译后生成的 HTML 和资源文件。

#### 启动开发工具

cd 到 ***your-project-name/src*** 目录，运行 ***jane dev*** ，将在 80 端口启动一个开发服务器，并自动打开浏览器预览页面。如果需要使用其他端口，可以运行 ***jane dev -p 8000*** 。

#### 编写普通文章

在 src/page/2020 目录下增加一篇文章 02-28-my-first-article.md ，增加以下内容：

    ---
    title: My first Article
    image: https://i.picsum.photos/id/927/2560/600.jpg
    category: IT
    ---

    This is My first Article 

保存文件，之后浏览器页面会自动更新，主页的文章列表中已经多了一项 ***My first Article*** 的条目，点击可以打开编写的文章。

#### 编写导航栏文章

在 src/page 目录下增加一篇文章 nav2-my-nav.md ，增加一下内容：

    ---
    title: My Nav Page
    image: https://picsum.photos/2560/600
    ---

    This is My Nav Page

保存文件后，主页的右上角导航栏已经多了一项 ***Nav*** 的条目，点击可以打开编写的文章。

#### 编译输出 HTML 文档及资源

文章编写完成后，在命令行窗口中敲一下回车，开发服务器将退出，同时通知浏览器关闭页面。之后，运行 ***jane build*** 命令，将 src/page 所有文章编译为 HTML 文档输出至 build 目录，同时，将 src/static 下的文件原样拷贝至 build 目录。此目录可以部署到网站的任何位置。

请勿修改和删除 build 目录下的文件，下次修改文章或资源后，重新运行 jane build ，只会更新需要修改过的文件。

#### 自定义页面框架和样式

如果需要调整页面的框架或者页面中元素的样式，可以修改 template.mustache 和 styles.css 等文件，前提是你已经熟悉 Mustache 模板语法、 HTML 语法 以及 CSS 语法。更进一步的，如果你熟悉 kotlin 语言，可以修改 [jane](https://github.com/pandolia/jane) 源码，打造自己的静态博客系统。如果你有任何的建议或想法，请给我发邮件 [pandolia@yeah.net](mailto://pandolia@yeah.net) 。