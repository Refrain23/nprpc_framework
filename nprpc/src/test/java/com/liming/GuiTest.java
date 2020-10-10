package com.liming;


/**
 * 事件回调机制测试，面向接口编程
 */
interface NotifyCallBack {
    void result(String file);
    void progress(String file, int progress);
}

public class GuiTest implements NotifyCallBack {
    private Download download;

    public GuiTest() {
        this.download = new Download(this);
    }

    @Override
    public void result(String file) {
        System.out.println(file + "下载完成！");
    }

    @Override
    public void progress(String file, int progress) {
        System.out.println(file + ":" + progress + "%");
    }

    public static void main(String[] args) {
        new GuiTest().download("我要学java");
    }

    private void download(String file) {
        System.out.println("开始下载文件：" + file);
        download.start(file);
    }
}

class Download {
    private NotifyCallBack notifyCallBack;

    public Download(NotifyCallBack notifyCallBack) {
        this.notifyCallBack = notifyCallBack;
    }

    public void start(String file) {
        int count = 0;
        try {
            while (count <= 100) {
                notifyCallBack.progress(file,count);
                Thread.sleep(500);
                count += 10;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        notifyCallBack.result(file);
    }
}


