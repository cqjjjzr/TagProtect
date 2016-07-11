package charlie.tagprotect;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.prefs.Preferences;

public class TagProtect {
    private static final String TAG_REQ_ROOT = "http://api.bilibili.com/x/tag/archive/tags?aid=";
    private static final String TAG_ADD_URL = "http://api.bilibili.com/x/tag/archive/add";

    private Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    protected LinkedList<String> list = new LinkedList<>();
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    private JPanel pnlContent;
    private JTextField txtAid;
    private JTextField txtInternal;
    private JLabel lblStatus;
    private JButton btnStart;
    private JTextField txtUID;
    private JTextField txtSessData;
    private JButton btnEditList;
    private JButton btnSave;
    private JButton btnForceFix;
    private JCheckBox chbAuto;
    private JCheckBox chbSlient;
    private TagListEdit dialog;

    private TagProtect(){
        txtAid.setText(preferences.get("aid", ""));
        txtInternal.setText(preferences.get("internal", "10"));
        txtUID.setText(preferences.get("DedeUserID", ""));
        txtSessData.setText(preferences.get("SESSDATA", ""));
        Collections.addAll(list, preferences.get("list", "").split(","));
        btnStart.addActionListener(e -> {
            btnStart.setEnabled(false);
            btnEditList.setEnabled(false);
            btnSave.setEnabled(false);
            new Thread(new TagChecker()).start();
            log("Started on " + txtAid.getText());
        });
        btnSave.addActionListener(e -> {
            preferences.put("aid", txtAid.getText());
            preferences.put("internal", txtInternal.getText());
            preferences.put("DedeUserID", txtUID.getText());
            preferences.put("SESSDATA", txtSessData.getText());
            StringBuilder sb = new StringBuilder();
            for(String tag : list){
                sb.append(tag);
                sb.append(',');
            }
            if(sb.length() > 0)
                sb.substring(0, sb.length() - 1);
            preferences.put("list", sb.toString());
            lblStatus.setText("已保存。");
            log("Saved.");
        });
        btnEditList.addActionListener(e -> dialog.setVisible(true));
        dialog = new TagListEdit(this);
        dialog.setBounds(10, 10, 200, 600);
        /*try {
            fixTags();
        } catch (Exception e) {
            e.printStackTrace();
        }*/ //测试代码.....
        btnForceFix.addActionListener(e -> {
            try {
                fixTags(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        log("Started.");
    }

    public static void main(String[] args) {
        log("Welcome to TagProtect by Charlie Jiang!");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        JFrame frame = new JFrame("TagProtect");
        frame.setContentPane(new TagProtect().pnlContent);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private class TagChecker implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
                    Thread.sleep(Integer.parseInt(txtInternal.getText()) * 1000);
                    checkTag();
                }
                catch (NumberFormatException e){lblStatus.setText("间隔格式错误！仅能为纯数字");}
                catch (Exception ex) {lblStatus.setText("Error:" + ex.toString()); log("Error:" + ex.toString());}
            }
        }
    }

    private static void log(String msg){
        try {
            Path path = Paths.get("tagprotect.log");
            if(!path.toFile().exists()) Files.createFile(path);
            Files.write(Paths.get("tagprotect.log"), ("[" + format.format(new Date()) + "][TagProtect] " + msg + "\n").getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private void checkTag() throws Exception {
        String url = TAG_REQ_ROOT + txtAid.getText();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if(connection.getResponseCode() == 200){
            byte[] buf = new byte[128];
            InputStream stream = connection.getInputStream();
            int readLength;
            while((readLength = stream.read(buf)) != -1){
                outputStream.write(buf, 0, readLength);
            }
            stream.close();
            String jsonStr = new String(outputStream.toByteArray());
            JSONObject jsonObj = new JSONObject(jsonStr);
            int code = jsonObj.getInt("code");
            if(code == -400){
                lblStatus.setText("av号格式错误！仅能为纯数字");
            }else if(code == 0){
                lblStatus.setText("OK");
                //log("OK!" + jsonStr);
                checkFull(jsonObj);
            }else if(code == 16006){
                lblStatus.setText("av号不存在或者Tag被清了QwQ");
                fixTags(false);
            }
        }else{
            lblStatus.setText("请求失败！" + connection.getResponseCode());
        }
        connection.disconnect();
    }

    private void checkFull(JSONObject jsonObj) throws Exception {
        JSONArray array = jsonObj.getJSONArray("data");
        int sum = 0;
        for(int i = 0;i < array.length();i++){
            JSONObject obj = array.getJSONObject(i);
            String tagName = obj.getString("tag_name");
            for(String tag : list) if(tag.equalsIgnoreCase(tagName)) sum++;
        }
        if(sum < list.size()) fixTags(false);
    }

    private void fixTags(boolean isdoi) throws Exception {
        boolean isdo;
        isdo = !(!isdoi && !chbAuto.isSelected()) || JOptionPane.showConfirmDialog(null, "av号不存在或者Tag被清了QwQ，是否还原？", "Question", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (isdo) {
            for (String tag : list) {
                if (tag.equals("")) continue;
                HttpURLConnection connection = (HttpURLConnection) new URL(TAG_ADD_URL).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Cookie", "DedeUserID=" + txtUID.getText() + "; SESSDATA=" + txtSessData.getText());
                connection.setDoOutput(true);
                connection.setDoInput(true);
                //connection.connect();
                PrintWriter out = new PrintWriter(connection.getOutputStream());
                out.write("aid=" + URLEncoder.encode(txtAid.getText(), "UTF-8") + "&tag_name=" + URLEncoder.encode(tag, "UTF-8"));
                out.flush();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (connection.getResponseCode() == 200) {
                    byte[] buf = new byte[128];
                    InputStream stream = connection.getInputStream();
                    int readLength;
                    while ((readLength = stream.read(buf)) != -1) {
                        outputStream.write(buf, 0, readLength);
                    }
                    stream.close();
                    String jsonStr = new String(outputStream.toByteArray());
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    int code = jsonObj.getInt("code");
                    if (code == 0) {
                        if (!chbSlient.isSelected())
                            JOptionPane.showMessageDialog(null, "成功添加Tag:" + tag, "Information", JOptionPane.INFORMATION_MESSAGE);
                        lblStatus.setText("成功添加Tag:" + tag);
                        log("Add successful:" + tag);
                    } else {
                        if (!chbSlient.isSelected())
                            JOptionPane.showMessageDialog(null, "添加Tag:" + tag + "失败，信息：" + jsonObj.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
                        lblStatus.setText("添加Tag:" + tag + "失败，信息：" + jsonObj.getString("message"));
                        log("Add failed:" + tag + " msg:" + jsonObj.getString("message"));
                    }
                }
                out.close();
                connection.disconnect();
            }
        }
    }
}
