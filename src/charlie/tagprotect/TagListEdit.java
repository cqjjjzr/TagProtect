package charlie.tagprotect;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.nio.file.Paths;

public class TagListEdit extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JList<String> lstTags;
    private JButton btnAdd;
    private JButton btnRemove;

    public TagListEdit(TagProtect parent) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> onOK());
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        lstTags.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstTags.setModel(new AbstractListModel<String>() {
            @Override
            public int getSize() {
                return parent.list.size();
            }

            @Override
            public String getElementAt(int index) {
                return parent.list.get(index);
            }
        });
        ActionListener listener = e -> {
            if(e.getSource() == btnAdd){
                String str = JOptionPane.showInputDialog(null, "tag内容：", "", JOptionPane.QUESTION_MESSAGE);
                if(str != null && !str.equals("")){
                    if(str.contains(",")){
                        JOptionPane.showMessageDialog(null, "tag不可含有逗号！", "Error", JOptionPane.ERROR_MESSAGE);
                    }else{
                        parent.list.add(str);
                        lstTags.updateUI();
                        lstTags.repaint();
                    }
                }
            }else{
                parent.list.remove(lstTags.getSelectedValue());
                lstTags.updateUI();
                lstTags.repaint();
            }
        };
        btnAdd.addActionListener(listener);
        btnRemove.addActionListener(listener);
    }

    private void onOK() {
        dispose();
    }

    public static void main(String[] args) {
        System.out.println(Paths.get("tagprotect.log").toAbsolutePath());
    }
}
