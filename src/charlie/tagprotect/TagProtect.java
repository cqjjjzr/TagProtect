package charlie.tagprotect;

import org.json.JSONObject;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.prefs.Preferences;

public class TagProtect {
    private static final String TAG_REQ_ROOT = "http://api.bilibili.com/x/tag/archive/tags?aid=";
    private static final String TAG_ADD_URL = "http://api.bilibili.com/x/tag/archive/add";

    private Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    protected LinkedList<String> list = new LinkedList<>();

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
    private TagListEdit dialog;

    private TagProtect(){
        txtAid.setText(preferences.get("aid", ""));
        txtInternal.setText(preferences.get("internal", "10"));
        txtUID.setText(preferences.get("DedeUserID", ""));
        txtSessData.setText(preferences.get("SESSDATA", ""));
        for(String tag : preferences.get("list", "").split(",")){
            list.add(tag);
        }
        btnStart.addActionListener(e -> {
            btnStart.setEnabled(false);
            btnEditList.setEnabled(false);
            btnSave.setEnabled(false);
            new Thread(new TagChecker()).start();
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
                fixTags();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
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
                catch (Exception ex) {lblStatus.setText("Error:" + ex.toString());}
            }
        }
    }

    public void checkTag() throws Exception {
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
            }else if(code == 16006){
                lblStatus.setText("av号不存在或者Tag被清了QwQ");
                fixTags();
            }
        }else{
            lblStatus.setText("请求失败！" + connection.getResponseCode());
        }
        connection.disconnect();
    }

    public void fixTags() throws Exception {
        boolean isdo = false;
        if(!chbAuto.isSelected()) isdo = JOptionPane.showConfirmDialog(null, "av号不存在或者Tag被清了QwQ，是否还原？", "Question", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        else isdo = true;
        if(isdo){
            for(String tag : list){
                if(tag.equals("")) continue;
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
                    if(code == 0){
                        JOptionPane.showMessageDialog(null, "成功添加Tag:" + tag, "Information", JOptionPane.INFORMATION_MESSAGE);
                    }else{
                        JOptionPane.showMessageDialog(null, "添加Tag:" + tag + "失败，信息：" + jsonObj.getString("message"), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                out.close();
                connection.disconnect();
            }
        }
    }
}
