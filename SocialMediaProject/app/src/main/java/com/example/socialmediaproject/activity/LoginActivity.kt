package com.example.socialmediaproject.activity

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.socialmediaproject.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var title: TextView
    private lateinit var signuptext: TextView
    private lateinit var intent: Intent
    private lateinit var loginbutton: Button
    private lateinit var firebaseauth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Thread.sleep(1500)
        installSplashScreen()
        setContentView(R.layout.login)
        title=findViewById(R.id.title)
        SetTitleGradientColor(title)
        signuptext=findViewById(R.id.signUpText)
        signuptext.setOnClickListener { OnSignUpClicked() }
        loginbutton=findViewById(R.id.loginButton)
        firebaseauth=FirebaseAuth.getInstance()
        loginbutton.setOnClickListener {
            val email=findViewById<TextInputEditText>(R.id.email)
            val password=findViewById<TextInputEditText>(R.id.password)
            if (email.text.toString().isNotEmpty() && password.text.toString().isNotEmpty())
            {
                loginbutton.isEnabled=false
                loginbutton.setBackgroundColor(resources.getColor(R.color.gray))
                db=FirebaseFirestore.getInstance()
                db.collection("Claims").whereEqualTo("useremail", email.text.toString()).get().addOnSuccessListener {
                    results->if (results!=null) {
                        Toast.makeText(this, "Đăng xuất tài khoản trên thiết bị khác để đăng nhập!", Toast.LENGTH_SHORT).show()
                    }
                    else {
                    firebaseauth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                        .addOnCompleteListener() {
                                task->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                try {
                                    db.collection("Claims").add(hashMapOf(
                                        "useremail" to email.text.toString()
                                    ))
                                    intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                catch(err: Exception) {
                                    Log.e("LoginError", "Lỗi khi chuyển sang MainActivity: ${err.message}")
                                }
                            }
                            else
                            {
                                Toast.makeText(this, "Tên đăng nhập/mật khẩu không chính xác hoặc không có internet!", Toast.LENGTH_SHORT).show()
                                loginbutton.isEnabled=true
                                loginbutton.setBackgroundColor(resources.getColor(R.color.purple_200))
                            }
                        }
                    }
                }
            }
            else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ các trường!", Toast.LENGTH_SHORT).show()
                loginbutton.isEnabled=true
            }
        }
    }

    private fun SetTitleGradientColor(title: TextView)
    {
        val paint = title.paint
        val width = title.paint.measureText(title.text.toString())
        val shader = LinearGradient(
            0f, 0f, width, 0f,
            intArrayOf(0xFF000000.toInt(), 0xFF800080.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    private fun OnSignUpClicked()
    {
        intent=Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }
}