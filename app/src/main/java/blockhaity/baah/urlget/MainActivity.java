package blockhaity.baah.urlget;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BROWSER_ROLE = 1001;
    private static final int REQUEST_MANAGE_STORAGE = 1002;
    private static final String TARGET_PATH =
            Environment.getExternalStorageDirectory() + "/url.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查存储权限
        if (needSpecialPermission()) {
            requestStoragePermission();
        } else {
            checkDefaultBrowserStatus();
            handleIntent(getIntent());
        }
    }

    // 检查是否需要特殊权限
    private boolean needSpecialPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager();
    }

    // 请求存储权限
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(this)
                    .setTitle("需要特殊权限")
                    .setMessage("请允许访问所有文件以保存到存储根目录")
                    .setPositiveButton("去设置", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                    })
                    .setNegativeButton("取消", (d, w) -> finish())
                    .show();
        }
    }

    // 保存文件到根目录（兼容 Android 10+）
    private void saveToRootDirectory(String url) {
        try {
            File file = new File(TARGET_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(url);
                showToast("已保存到存储根目录：" + file.getAbsolutePath());
            }
        } catch (IOException e) {
            showToast("保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 判断当前应用是否为默认浏览器
     */
    private void checkDefaultBrowserStatus() {
        if (!isDefaultBrowser()) {
            showSetDefaultDialog();
        }
    }

    private void showSetDefaultDialog() {
        new AlertDialog.Builder(this)
                .setTitle("设为默认浏览器")
                .setMessage("需要将本应用设置为默认浏览器才能捕获URL")
                .setPositiveButton("设置", (dialog, which) -> requestBrowserRole())
                .setNegativeButton("取消", (dialog, which) -> {
                    Toast.makeText(this, "必须设为默认浏览器才能使用", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private boolean isDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用RoleManager检查
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            if (roleManager != null) {
                return roleManager.isRoleHeld(RoleManager.ROLE_BROWSER);
            }
        } else {
            // Android 9及以下通过解析默认浏览器包名
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            ResolveInfo resolveInfo = getPackageManager().resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
            );

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                return getPackageName().equals(resolveInfo.activityInfo.packageName);
            }
        }
        return false;
    }

    /**
     * 请求成为默认浏览器
     */
    private void requestBrowserRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用RoleManager API
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER);
                startActivityForResult(intent, REQUEST_CODE_BROWSER_ROLE);
            }
        } else {
            // Android 9 打开默认应用设置
            Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "请到系统设置中设为默认浏览器", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BROWSER_ROLE) {
            // 处理Android 10+的角色请求结果
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (resultCode == Activity.RESULT_OK) {
                    // 用户已授权
                    handleIntent(getIntent());
                } else {
                    // 用户拒绝
                    Toast.makeText(this, "必须设为默认浏览器才能使用", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                saveToRootDirectory(uri.toString());
            }
        }
    }

    private void showToast(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}