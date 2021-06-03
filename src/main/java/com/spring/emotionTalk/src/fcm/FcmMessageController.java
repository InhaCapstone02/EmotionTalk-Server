package com.spring.emotionTalk.src.fcm;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.spring.emotionTalk.config.BaseException;
import com.spring.emotionTalk.config.BaseResponse;
import com.spring.emotionTalk.src.S3Service;
import com.spring.emotionTalk.src.auth.service.OauthService;
import com.spring.emotionTalk.src.fcm.model.PostChatReq;
import com.spring.emotionTalk.src.user.model.PostUserReq;
import com.spring.emotionTalk.src.user.model.PostUserRes;
import com.spring.emotionTalk.utils.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.print.BackgroundServiceLookup;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import static com.spring.emotionTalk.config.BaseResponseStatus.*;

@RestController
@RequestMapping("/app")
public class FcmMessageController {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private final OauthService oauthService;
    @Autowired
    private final JwtService jwtService;
    @Autowired
    private final S3Service s3Service;
    @Autowired
    private final FirebaseCloudMessageService firebaseCloudMessageService;
    @Autowired
    private final FcmProvider fcmProvider;

    public FcmMessageController(OauthService oauthService, JwtService jwtService, S3Service s3Service,
                                FirebaseCloudMessageService firebaseCloudMessageService,
                                FcmProvider fcmProvider){
        this.oauthService = oauthService;
        this.jwtService = jwtService;
        this.s3Service = s3Service;
        this.firebaseCloudMessageService = firebaseCloudMessageService;
        this.fcmProvider = fcmProvider;
    }

    @ResponseBody
    @PostMapping("/chat/img")
    public BaseResponse sendImg(
            @RequestPart (value="imgFile", required = false)MultipartFile imgFile,
            @RequestParam (value = "anotherKey") Integer anotherKey
            ) throws BaseException, IOException, FirebaseMessagingException {

        int userKey = jwtService.getUserKey();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if(imgFile == null || anotherKey == null)
            return new BaseResponse(INVALID_INPUT);

        if(fcmProvider.isValidUserKey(anotherKey)==0)
            return new BaseResponse(INVALID_USER_KEY);

        String myDeviceToken = fcmProvider.getDeviceTokenByUserKey(userKey);
        String anotherDeviceToken = fcmProvider.getDeviceTokenByUserKey(anotherKey);

        S3Service s3Service = new S3Service();
        String img = s3Service.upload(imgFile);

        firebaseCloudMessageService.sendMessageTo(
                myDeviceToken,
                fcmProvider.getUserNameByUserKey(userKey),
                userKey,
                fcmProvider.getUserNameByUserKey(anotherKey),
                anotherKey,
                "이미지 전송",
                img,
                "",
                now);

        firebaseCloudMessageService.sendMessageTo(
                anotherDeviceToken,
                fcmProvider.getUserNameByUserKey(userKey),
                userKey,
                fcmProvider.getUserNameByUserKey(anotherKey),
                anotherKey,
                "이미지 전송",
                img,
                "",
                now);

        String result = "전송하였습니다.";
        return new BaseResponse(result);
    }


    @ResponseBody
    @PostMapping("/chat")
    public BaseResponse sendMessage(@RequestBody PostChatReq postChatReq)
            throws IOException, BaseException, FirebaseMessagingException {

        int userKeyByJwt = jwtService.getUserKey();

        if(postChatReq.getContents()==null || postChatReq.getAnotherKey() == null || postChatReq.getEmotion()==null)
            return new BaseResponse(INVALID_INPUT);

        if(fcmProvider.isValidUserKey(postChatReq.getAnotherKey())==0)
            return new BaseResponse(INVALID_USER_KEY);

        Timestamp now = new Timestamp(System.currentTimeMillis());

        String myDeviceToken = fcmProvider.getDeviceTokenByUserKey(userKeyByJwt);
        String anotherDeviceToken = fcmProvider.getDeviceTokenByUserKey(postChatReq.getAnotherKey());

        firebaseCloudMessageService.sendMessageTo(
                anotherDeviceToken,
                fcmProvider.getUserNameByUserKey(userKeyByJwt),
                userKeyByJwt,
                fcmProvider.getUserNameByUserKey(postChatReq.getAnotherKey()),
                postChatReq.getAnotherKey(),
                postChatReq.getContents(),
                "",
                postChatReq.getEmotion(),
                now);

        firebaseCloudMessageService.sendMessageTo(
                myDeviceToken,
                fcmProvider.getUserNameByUserKey(userKeyByJwt),
                userKeyByJwt,
                fcmProvider.getUserNameByUserKey(postChatReq.getAnotherKey()),
                postChatReq.getAnotherKey(),
                postChatReq.getContents(),
                "",
                postChatReq.getEmotion(),
                now);

        String result = "전송하였습니다.";
        return new BaseResponse(result);
    }

}
