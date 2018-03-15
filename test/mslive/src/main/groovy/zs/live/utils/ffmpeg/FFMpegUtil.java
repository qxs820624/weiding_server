package zs.live.utils.ffmpeg;

import zs.live.utils.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FFMpegUntil
 * <p>Title: FFMpeg工具类</p>
 * <p>Description: 本类封装FFMpeg对视频的操作</p>
 */
public class FFMpegUtil implements IStringGetter{

    private String ffmpegUri = "/data01/ffmpeg/bin/ffmpeg";//ffmpeg的全路径
    private List<String> cmd = new ArrayList<String>();
    private enum FILE_TYPE{TS, FLV, MP4};//文件类型
    private enum CMD_TYPE{empty,mergeFile};//可执行回调的命令
    private CMD_TYPE CMD_STATUS = CMD_TYPE.empty;//当前执行命令

    public FFMpegUtil(){
        super();
    }

    /**
     * 构造函数
     * @param ffmpegUri ffmpeg的全路径
     * 		如e:/ffmpeg/ffmpeg.exe 或 /data01/ffmpeg/bin/ffmpeg
     */
    public FFMpegUtil(String ffmpegUri){
        this.ffmpegUri = ffmpegUri;
    }

    /**
     * 合并视频
     * @param files 待合并文件地址
     * @param resultName 合并后的名称（含目录）
     */
    private void mergeFile(List<String> files,String resultName){
        CMD_STATUS = CMD_TYPE.mergeFile;
        cmd.clear();
        cmd.add(ffmpegUri);
        cmd.add("-y");//覆盖输出文件而不询问
        cmd.add("-i");//输入文件名
        cmd.add("concat:" + Strings.toListString(files, "|"));
        cmd.add("-c");
        cmd.add("copy");
//        cmd.add("-s");//分辨率
//        cmd.add("480*272");//分辨率 宽x高，如：1920×1080，1280×720，480*272
//        cmd.add("-aspect");
//        cmd.add("16:9");
        cmd.add(resultName);
        CmdExecuter.exec(cmd, this);
    }

    /**
     * 文件转为ts格式
     * @param file 源文件
     * @param type 转换的目标类型 TS, FLV, MP4
     * @return
     */
    private String convertFile(String file,Enum type){
        CMD_STATUS = CMD_TYPE.empty;
        String prefix = file.substring(0,file.indexOf("."));
        String resultName = prefix + "_" + System.currentTimeMillis()+"_temp."+type;
        cmd.clear();
        cmd.add(ffmpegUri);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(file);
        cmd.add("-c");
        cmd.add("copy");
//        cmd.add("-s");//分辨率
//        cmd.add("1920x1080");//分辨率 宽x高，如：1920×1080，1280×720，480*272
        cmd.add(resultName);
        CmdExecuter.exec(cmd, this);
        return resultName;
    }

    /**
     * 安全合并视频文件，目前仅支持mp4的输出
     * @param files
     * @param resultName
     */
    public void safeMergeFile(List<String> files,String resultName){
        CMD_STATUS = CMD_TYPE.empty;
        List<String> newList = new ArrayList<>();
        for (String file : files) {
            String newName = this.convertFile(file, FILE_TYPE.TS);
            newList.add(newName);
        }
        this.mergeFile(newList, resultName);
//        this.delFiles(files);//删除下载文件
        this.delFiles(newList);//删除临时文件
    }

    /**
     * 执行命令的回调
     * @param str 执行命令返回的参数
     */
    @Override
    public void dealString(String str){
        switch (CMD_STATUS){
            case mergeFile:
                //frame=  181 fps=0.0 q=-1.0 Lsize=     525kB time=00:00:07.52 bitrate= 571.3kbits/s speed=1.58e+003x
                Matcher m = Pattern.compile("time=\\d+:\\d+:\\d+\\.\\d*").matcher(str);
                while (m.find()) {
                    String msg = m.group();
                    msg = msg.replace("time=","");
//                    System.out.println("result------>>>>"+msg);
                }
                break;
            default:
//                System.out.println(str);
                break;
        }
    }

    /**
     * 删除temp文件
     * @param files 待删除文件
     */
    private void delFiles(List<String> files){
        for (String file : files) {
            File f = new File(file);
            f.delete();
        }
    }

    /**
     * 测试
     */
    private void testCommand(){
        cmd.clear();
        cmd.add("java");
        cmd.add("-version");
        CmdExecuter.exec(cmd, this);
    }

//    public static void main(String[] args) {
//        String dir = "F:/FFResult/";
//        List<String> list = new ArrayList<>();
//        list.add(dir+"1-3.ts");
//        list.add(dir+"1-4.ts");
//
//        new FFMpegUtil("D:/SOFTS/ffmpeg/bin/ffmpeg").safeMergeFile(list, dir+"abc.mp4");
//
////        String a = new FFMpegUtil("D:/SOFTS/ffmpeg/bin/ffmpeg").convert2TS("F:/QQMiniDL/111.mp4");
////        System.out.println("----->>>>"+a);
//    }
}
