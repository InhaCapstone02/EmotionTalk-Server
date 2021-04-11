    package com.spring.emotionTalk.src.user;

import com.spring.emotionTalk.src.auth.dto.AuthDto;
import com.spring.emotionTalk.src.auth.helper.constants.SocialLoginType;
import com.spring.emotionTalk.src.auth.service.OauthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.spring.emotionTalk.config.BaseException;
import com.spring.emotionTalk.config.BaseResponse;
import com.spring.emotionTalk.src.user.model.*;
import com.spring.emotionTalk.utils.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.util.List;


import static com.spring.emotionTalk.config.BaseResponseStatus.*;

@RestController
@RequestMapping("/app")
public class UserController {
    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OauthService oauthService;

    @Autowired
    private final UserProvider userProvider;
    @Autowired
    private final UserService userService;
    @Autowired
    private final JwtService jwtService;

    public UserController(OauthService oauthService, UserProvider userProvider, UserService userService, JwtService jwtService){
        this.oauthService = oauthService;
        this.userProvider = userProvider;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * 회원 조회 API
     * [GET] /users
     * 회원 번호 및 이메일 검색 조회 API
     * [GET] /users?userIdx= && Email=
     * @return BaseResponse<List<GetUserRes>>
     */
    //Query String
    @ResponseBody
    @GetMapping("/users") // (GET) 127.0.0.1:9000/app/users           @RequestParam = 쿼리스트링 받아옴
                                            //                  required = false => 쿼리스트링 안받아도 오류 x
    public BaseResponse<List<GetUserRes>> getUsers(@RequestParam(required = false) String Email) {
        // Get Users
        List<GetUserRes> getUsersRes = userProvider.getUsers(Email);
        return new BaseResponse<>(getUsersRes);
    }

    /**
     * 회원 1명 조회 API
     * [GET] /users/:userIdx
     * @return BaseResponse<GetUserRes>
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/user/{userIdx}") // (GET) 127.0.0.1:9000/app/users/:userIdx
    public BaseResponse<GetUserRes> getUser(@PathVariable("userIdx") int userIdx) {
        // Get Users
        GetUserRes getUserRes = userProvider.getUser(userIdx);
        return new BaseResponse<>(getUserRes);
    }

    /**
     * 회원가입 API
     * [POST] /user
     * @return BaseResponse<PostUserRes>
     */
    // Body
    @ResponseBody
    @PostMapping("/user")
    public BaseResponse<PostUserRes> createUser(@RequestBody PostUserReq postUserReq) throws BaseException {
        if(postUserReq.getName() == null){
            return new BaseResponse<>(POST_USER_EMPTY_NAME);
        }
        if(postUserReq.getPassword() == null){
            return new BaseResponse<>(POST_USER_EMPTY_PWD);
        }
        try{
            PostUserRes postUserRes = userService.createUser(postUserReq);
            return new BaseResponse<>(postUserRes);
        } catch(BaseException exception){
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /**
     * 로그인 API
     * [POST] /user/login
     * @return BaseResponse<PostUserRes>
     */
    @ResponseBody
    @PostMapping("/user/login")
    public BaseResponse<PostLoginRes> login(@RequestBody PostLoginReq postLoginReq) throws BaseException {
        try{
            if(postLoginReq.getName() == null) {
                return new BaseResponse<>(POST_USER_EMPTY_NAME);
            }
            if(postLoginReq.getPassword() == null) {
                return new BaseResponse<>(POST_USER_EMPTY_PWD);
            }
            PostLoginRes postLoginRes = userProvider.login(postLoginReq);
            return new BaseResponse<>(postLoginRes);

        }catch(BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     * 소셜 로그인 API
     * [POST] /{socialLoginType}/login
     * @param socialLoginType (GOOGLE, KAKAO)
     * @return BaseResponse<postLoginRes>
     */
    @ResponseBody
    @PostMapping("/{socialLoginType}/login")
    public BaseResponse<PostLoginRes> getInfo(
            @PathVariable(name = "socialLoginType") SocialLoginType socialLoginType,
            @RequestParam(name = "id_token") String idToken) throws BaseException, GeneralSecurityException, IOException {

        if (idToken == "") {
            return new BaseResponse<>(EMPTY_ID_TOKEN);
        } else {
            AuthDto.GoogleProfileRes googleProfileRes = oauthService.loadInfo(socialLoginType, idToken);

            GoogleUserReq googleUserReq = new GoogleUserReq();

            googleUserReq.setId(googleProfileRes.getId());
            googleUserReq.setEmail(googleProfileRes.getEmail());
            googleUserReq.setName(googleProfileRes.getName());

            PostLoginRes postLoginRes = new PostLoginRes();
            if (userService.findUserByGoogleEmail(googleUserReq.getEmail()) == 1) {
                System.out.println("loginUser");
                try {
                    postLoginRes = userProvider.logIn(googleUserReq.getEmail());
                    return new BaseResponse<>(postLoginRes);
                } catch (BaseException exception) {
                    return new BaseResponse<>(exception.getStatus());
                }
            } else {
                System.out.println("createUser");
                try {
                    postLoginRes = userService.createUserByGoogle(googleUserReq);
                    return new BaseResponse<>(postLoginRes);
                } catch (BaseException exception) {
                    return new BaseResponse<>(exception.getStatus());
                }
            }

        }
    }
}
