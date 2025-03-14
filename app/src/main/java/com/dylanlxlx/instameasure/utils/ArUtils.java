package com.dylanlxlx.instameasure.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

/**
 * ARCore工具类
 * 处理ARCore可用性检测和安装
 */
public class ArUtils {
    /**
     * 检查ARCore是否可用
     * @param context 上下文
     * @return 是否可用
     */
    public static boolean isArCoreAvailable(Context context) {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);
        return availability.isSupported();
    }

    /**
     * 请求安装ARCore（如果未安装）
     * @param activity 活动
     * @return 是否需要用户安装ARCore
     */
    public static boolean requestArCoreInstall(Activity activity) {
        try {
            // 请求安装/更新ARCore
            ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance()
                    .requestInstall(activity, true);

            switch (installStatus) {
                case INSTALLED:
                    // ARCore已安装
                    return true;

                case INSTALL_REQUESTED:
                    // 已请求安装，等待用户确认
                    Toast.makeText(activity, "请安装ARCore以使用AR功能", Toast.LENGTH_LONG).show();
                    return false;
            }

        } catch (UnavailableUserDeclinedInstallationException e) {
            // 用户拒绝安装
            Toast.makeText(activity, "AR功能需要安装ARCore", Toast.LENGTH_LONG).show();
        } catch (UnavailableDeviceNotCompatibleException e) {
            // 设备不兼容
            Toast.makeText(activity, "您的设备不支持AR功能", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // 其他错误
            Toast.makeText(activity, "AR初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return false;
    }
}