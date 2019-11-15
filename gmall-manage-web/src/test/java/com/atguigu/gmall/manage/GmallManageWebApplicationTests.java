package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

	@Test
	public void contextLoads() throws IOException, MyException {
		String tracker = GmallManageWebApplication.class.getResource("/tracker.conf").getPath();
		ClientGlobal.init(tracker);
		TrackerClient trackerClient=new TrackerClient();
		TrackerServer trackerServer=trackerClient.getConnection();
		StorageClient storageClient=new StorageClient(trackerServer,null);
		String orginalFilename="d:/image.png";
		String[] upload_file = storageClient.upload_file(orginalFilename, "png", null);
		for (String uploadFile : upload_file) {
			System.out.println(uploadFile);
		}
	}

}
