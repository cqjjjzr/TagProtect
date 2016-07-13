# TagProtect
监听并自动恢复Bilibili某视频的Tags.

送给soda纯白大大。同时也对那些天天删别人视频Tag的人说，呵呵！

##使用方法
- av号……就是av号啊，注意不要av，只要数字；
- 间隔是每次检查的间隔，看情况填。
- DedeUserID和SESSDATA是b站识别身份的ID，后面再讲如何提取。

接下来你需要打开编辑列表，添加所有你需要的Tag。
最后保存，然后点击Start，OK！当然，你也可以自己手动修复Tag。

##提取DedeUserID和SESSDATA
打开某个BiliBili视频，按F12，如果浏览器支持的话应该会弹出来开发工具，找到Console或者控制台，复制以下代码：
function GetAllCookie(){var aCookie = document.cookie.split("; ");for (var i=0; i < aCookie.length; i++){var aCrumb = aCookie[i].split("=");if(aCrumb[0] == "DedeUserID" || aCrumb[0] == "SESSDATA"){console.log(aCrumb);}}}
回车，然后输入GetAllCookie(),你会得到两个数组对吧，格式大概是["SESSDATA", "xxxxxxxxxx"]，把后面xxxxxxxxxx的内容复制到相应的位置（DedeUserID复制到DedeUserID里面，SESSDATA同理），OK。

##已知问题
~~只有Tags被清空的时候才会触发修复...下版本解析一下返回的tags来处理一下吧....~~已修复
