package com.example.google_backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtil {


    // 格式化文件大小为可读的字符串表示
    public static String formatFileSize(long sizeInBytes) {
        // 检查文件大小是否为负数
        if (sizeInBytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative.");
        }

        // 如果文件大小小于1KB，直接返回字节数
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }
        // 如果文件大小小于1MB，转换为KB并返回
        else if (sizeInBytes < 1024 * 1024) {
            double sizeInKB = sizeInBytes / 1024.0;
            return String.format("%.2f KB", sizeInKB);
        }
        // 如果文件大小小于1GB，转换为MB并返回
        else if (sizeInBytes < 1024 * 1024 * 1024) {
            double sizeInMB = sizeInBytes / (1024.0 * 1024);
            return String.format("%.2f MB", sizeInMB);
        }
        // 如果文件大小大于或等于1GB，转换为GB并返回
        else {
            double sizeInGB = sizeInBytes / (1024.0 * 1024 * 1024);
            return String.format("%.2f GB", sizeInGB);
        }
    }

    /**
     * 检查指定路径的文件夹是否存在
     *
     * @param localPath 本地路径
     * @return 如果文件夹存在返回true，否则返回false
     */
    public static boolean checkDirectoryExists(String localPath) {
        File directory = new File(localPath);
        return directory.exists() && directory.isDirectory();
    }


    public static long getFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles(); // 获取文件夹下所有文件和子目录

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // 如果是文件，则累加其大小
                    size += file.length();
                } else if (file.isDirectory()) {
                    // 如果是子目录，则递归计算其大小
                    size += getFolderSize(file);
                }
            }
        }
        return size;
    }



    public static void renameFile(String oldPath ,String newPath){
        File oldFolder = new File(oldPath);

        // 新文件夹路径
        File newFolder = new File(newPath);

        // 重命名文件夹
        boolean renamed = oldFolder.renameTo(newFolder);
    }
}
