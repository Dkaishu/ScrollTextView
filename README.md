# ScrollTextView
[![](https://jitpack.io/v/helen-x/JitpackReleaseDemo.svg)](https://jitpack.io/#Dkaishu/ScrollTextView)
### An Android Vertical Scrollable TextView;
### Android 垂直滚动展示的 TextView ;
 欢迎提 issues ☝☝

![image](https://github.com/Dkaishu/ScrollTextView/blob/master/example.gif)

- 继承自 View,可使用 View 相关属性；
- 可设置：字体大小颜色、滚动动画速度（时间）、停留显示时间、是否单行显示、单行显示是否带有省略号
- 可监听：不同文字的点击事件、滚动动画开始和结束
- 只有一个类文件，若不想依赖方式使用，将ScrollTextView.class 和 scroll_textview_attrs.xml 添加到本地即可

How to

##### To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {

	        compile 'com.github.Dkaishu:ScrollTextView:V1.2.3'
	}

Step 3. Use it in your code

       // .xml file:
       
       <com.dkaishu.scrolltextview.ScrollTextView
           xmlns:scroll_text="http://schemas.android.com/apk/res-auto"
           android:id="@+id/stv_example"
           android:layout_width="match_parent"
           android:layout_height="30dp"
           android:layout_gravity="center_vertical"
           android:padding="5dp"
           scroll_text:ellipsis="false"
           scroll_text:singleLine="true"
           scroll_text:textColor="@android:color/black"
           scroll_text:textSize="14sp"
           />
        
        
        
        
        //java file:
        
        ScrollTextView stvExample = (ScrollTextView) findViewById(R.id.stv_example);

        List<String> textList = new ArrayList<>();
        //note : clickListener 、scrollListener can be null ;
        List<ScrollTextView.OnScrollClickListener> clickListeners = new ArrayList<>();
        List<ScrollTextView.OnScrollListener> scrollListeners = new ArrayList<>();

        textList.add("The adolescent girl from Tennessee is standing on the stage of a drama summer camp in upstate New York. It's a beautiful day. But the girl doesn't feel beautiful. She's not the leggy, glamorous Hollywood type.");
        textList.add("一名少女由田纳西州来到纽约北部，她站在戏剧夏令营的舞台上，虽然天气是那么好，她的心情却一点也不好。");


        clickListeners.add(new ScrollTextView.OnScrollClickListener() {
            @Override
            public void onClick() {
                Toast.makeText(MainActivity.this, "this is text one", Toast.LENGTH_SHORT).show();
            }
        });
        
        clickListeners.add(new ScrollTextView.OnScrollClickListener() {
            @Override
            public void onClick() {
                Toast.makeText(MainActivity.this, "this is text two", Toast.LENGTH_SHORT).show();
            }
        });


        scrollListeners.add(new ScrollTextView.OnScrollListener() {
            @Override
            public void onScrollStart(List<ScrollTextView.TextInfo> passedTextInfos) {
                String text = "";
                for (ScrollTextView.TextInfo s : passedTextInfos) {
                    text = text + s.getText();
                }
                Log.e(TAG, "" + text);
            }
            @Override
            public void onScrollEnd(List<ScrollTextView.TextInfo> incommingTextInfos) {
                String text = "";
                for (ScrollTextView.TextInfo s : incommingTextInfos) {
                    text = text + s.getText();
                }
                Log.e(TAG, "" + text);
            }
        });
        
    //        stvExample.setScrollTime(500);//ms
    //        stvExample.setSpanTime(3000);//ms
    //        stvExample.setTextColor();
    //        stvExample.setTextSize();
            
           //Auto start,so you don't need this line,unless restart.
    //        stvExample.startTextScroll();
   
           //you can also setTextContent in this way below.
            
    //        stvExample.setTextContent(textList, clickListeners);
    //        stvExample.setTextContent(textList);
    //        stvExample.setTextContent(textList, null, scrollListeners);
            
   And you may need Override methods in your activity :
   
       @Override
       protected void onRestart() {
           super.onRestart();
           stvExample.startTextScroll();
       }
   
       @Override
       protected void onStop() {
           super.onStop();
           stvExample.stopTextScroll();
       }
       
   That's it! 