package edu.ucr.ece.btdatacomm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 登录的时候，用户输入的名和密码应该登录智能家居的网络服务器（控制板）
 * 需要网络服务器的验证，提高智能家居的安全性
 * @author Yanzeng
 *
 */
public class Login extends Activity {

    public static String filename = "MySharedString";
    SharedPreferences someData;
    EditText userName, passWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        initVars();
    }

    public void initVars(){
        userName = (EditText)findViewById(R.id.etUser);
        passWord = (EditText)findViewById(R.id.etPass);


    }

    //设置登录密码
    public void onBtnSetPass(View v){
        SharedPreferences.Editor editor = someData.edit();
        editor.clear();
        editor.putString("username", userName.getText().toString());
        editor.putString("password", passWord.getText().toString());
        editor.commit();
    }

    public void onBtnLogin(View v){
        //step1,获取用户输入的用户名和密码
        String inputuser = userName.getText().toString();
        String inputpass = passWord.getText().toString();

        //step2,提取正确的用户名和密码
        someData = getSharedPreferences(filename, 0);
        String user = someData.getString("username", "Couldn't load data!");
        String pass = someData.getString("password", "nothing");

        //step3,用户输入的用户名和密码  vs.正确的用户名和密码
        if(user.contentEquals(inputuser) && pass.contentEquals(inputpass)){
            try {
                Class ourClass = Class.forName("edu.ucr.ece.btdatacomm.MainPage");
                Intent ourIntent = new Intent(Login.this, ourClass);
                startActivity(ourIntent);
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            //用户名或密码输入错误的提示
            Toast toast = Toast.makeText(Login.this,
                    "Username or Password is wrong, please enter again.", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

    public void onBtnCancel(View v){
        finish();
    }
}
