package com.example.demo.upload;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileUpAndDownServiceImpl implements FileUpAndDownService {

    private Logger logger = LoggerFactory.getLogger(FileUpAndDownServiceImpl.class);

    @Autowired
    private MessageProperties config; //用来获取file-message.properties配置文件中的信息

    @Override
    public Map<String, Object> uploadPicture(MultipartFile file) throws ServiceException {
        try {
            Map<String, Object> resMap = new HashMap<>();
            String[] IMAGE_TYPE = config.getImageType().split(",");
            String path = null;
            boolean flag = false;
            for (String type : IMAGE_TYPE) {
                if (StringUtils.endsWithIgnoreCase(file.getOriginalFilename(), type)) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                resMap.put("result", IStatusMessage.SystemStatus.SUCCESS.getMessage());
                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                // 获得文件类型
                String fileType = file.getContentType();
                // 获得文件后缀名称
                String imageName = fileType.substring(fileType.indexOf("/") + 1);
                // 原名称
                String oldFileName = file.getOriginalFilename();
                // 新名称
                String newFileName = uuid + "." + imageName;
                // 年月日文件夹
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String basedir = sdf.format(new Date());
                // 重新生成
                String newUUID = UUID.randomUUID().toString().replaceAll("-", "");
                newFileName = newUUID + "." + imageName;
                path = config.getUpPath() + "/projectImg" + "/" + basedir + "/";
                // 如果目录不存在则创建目录
                File oldFile = new File(path);
                if (!oldFile.exists()) {
                    oldFile.mkdirs();
                }
                oldFile = new File(path + newUUID + "." + imageName);
                if (!oldFile.exists()) {
                    oldFile.createNewFile();
                }
                //保存文件
                //使用此方法保存必须要绝对路径且文件夹必须已存在,否则报错
                file.transferTo(oldFile);
                // 进行压缩(大于4M)
                if (file.getSize() > config.getFileSize()) {
                    // 压缩图片
                    Thumbnails.of(oldFile).scale(config.getScaleRatio()).toFile(path);
                }
                // 显示路径
                resMap.put("path", "/" + basedir + "/" + newUUID + "." + imageName);
                resMap.put("oldFileName", oldFileName);
                resMap.put("newFileName", newFileName);
                resMap.put("fileSize", file.getSize());
            } else {
                resMap.put("result", "图片格式不正确,支持png|jpg|jpeg");
            }
            return resMap;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }
}
