package com.zy.blog.web.controller;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.blog.commons.feign.PictureFeignClient;
import com.zy.blog.entity.FeedBack;
import com.zy.blog.entity.Link;
import com.zy.blog.entity.SystemConfig;
import com.zy.blog.entity.User;
import com.zy.blog.service.netutils.RabbitMqUtil;
import com.zy.blog.service.netutils.WebUtil;
import com.zy.blog.service.service.*;
import com.zy.blog.utils.ThrowableUtils;
import com.zy.blog.utils.annotion.validator.group.Insert;
import com.zy.blog.utils.constant.Constants;
import com.zy.blog.utils.constant.MessageConf;
import com.zy.blog.utils.constant.RedisConf;
import com.zy.blog.utils.constant.SQLConf;
import com.zy.blog.utils.enums.EGender;
import com.zy.blog.utils.enums.ELinkStatus;
import com.zy.blog.utils.enums.EOpenStatus;
import com.zy.blog.utils.enums.EStatus;
import com.zy.blog.utils.util.*;
import com.zy.blog.view.FeedBackView;
import com.zy.blog.view.FileView;
import com.zy.blog.view.LinkView;
import com.zy.blog.view.UserView;
import com.zy.blog.web.global.SysConf;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
//import me.zhyd.oauth.config.AuthConfig;
//import me.zhyd.oauth.model.AuthCallback;
//import me.zhyd.oauth.model.AuthResponse;
//import me.zhyd.oauth.model.AuthToken;
//import me.zhyd.oauth.request.AuthGiteeRequest;
//import me.zhyd.oauth.request.AuthGithubRequest;
//import me.zhyd.oauth.request.AuthQqRequest;
//import me.zhyd.oauth.request.AuthRequest;
//import me.zhyd.oauth.utils.AuthStateUtils;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ?????????????????????
 *
 * @author ?????????
 * @date 2021???10???11???10:25:58
 */
@RestController
@RefreshScope
@RequestMapping("/oauth")
@Api(value = "????????????", tags = {"????????????"})
@Slf4j
public class AuthRestApi {
    @Autowired
    private WebUtil webUtil;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private WebConfigService webConfigService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private LinkService linkService;
    @Autowired
    private RabbitMqUtil rabbitMqUtil;
    @Autowired
    private UserService userService;
    /*@Value(value = "${justAuth.clientId.gitee}")*/
    private String giteeClienId;
   /* @Value(value = "${justAuth.clientSecret.gitee}")*/
    private String giteeClientSecret;
    /*@Value(value = "${justAuth.clientId.github}")*/
    private String githubClienId;
  /*  @Value(value = "${justAuth.clientSecret.github}")*/
    private String githubClientSecret;
   /* @Value(value = "${justAuth.clientId.qq}")*/
    private String qqClienId;
    /*@Value(value = "${justAuth.clientSecret.qq}")*/
    private String qqClientSecret;
    /*@Value(value = "${data.webSite.url}")*/
    private String webSiteUrl;
  /*  @Value(value = "${data.web.url}")*/
    private String moguWebUrl;
    /*@Value(value = "${BLOG.USER_TOKEN_SURVIVAL_TIME}")*/
    private Long userTokenSurvivalTime =new Long(720000);
    /**
     * ???????????????
     */
    /*@Value(value = "${data.web.project_name_en}")*/
    private String PROJECT_NAME_EN;
   /* @Value(value = "${DEFAULE_PWD}")*/
    private String DEFAULE_PWD;
   /* @Value(value = "${uniapp.qq.appid}")*/
    private String APP_ID;
    /*@Value(value = "${uniapp.qq.appid}")*/
    private String SECRET;
    /*@Value(value = "${uniapp.qq.grant_type}")*/
    private String GRANT_TYPE;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PictureFeignClient pictureFeignClient;

    @ApiOperation(value = "????????????", notes = "????????????")
    @RequestMapping("/render")
    public String renderAuth(String source) {
        // ?????????????????????????????????
        Boolean isOpenLoginType = webConfigService.isOpenLoginType(source.toUpperCase());
        if (!isOpenLoginType) {
            return ResultUtil.result(SysConf.ERROR, "??????????????????????????????!");
        }
        log.info("??????render:" + source);
        AuthRequest authRequest = getAuthRequest(source);
        String token = AuthStateUtils.createState();
        String authorizeUrl = authRequest.authorize(token);
        Map<String, String> map = new HashMap<>();
        map.put(SQLConf.URL, authorizeUrl);
        return ResultUtil.result(SysConf.SUCCESS, map);
    }


    /**
     * oauth?????????????????????????????????????????????????????????????????????gitee???????????????????????????????????????http://127.0.0.1:8603/oauth/callback/gitee
     */
    @RequestMapping("/callback/{source}")
    public void login(@PathVariable("source") String source/*AuthCallback callback*/, HttpServletResponse httpServletResponse) throws IOException {


    }

    /**
     * ??????????????????
     *
     * @param data
     * @param user
     */
    private void updateUserPhoto(Map<String, Object> data, User user) {
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(SQLConf.STATUS, EStatus.ENABLE);
        queryWrapper.last(com.zy.blog.utils.constant.SysConf.LIMIT_ONE);
        SystemConfig systemConfig = systemConfigService.getOne(queryWrapper);
        // ????????????????????????????????????????????????
        FileView fileVO = new FileView();
        fileVO.setAdminUid(com.zy.blog.utils.constant.SysConf.DEFAULT_UID);
        fileVO.setUserUid(com.zy.blog.utils.constant.SysConf.DEFAULT_UID);
        fileVO.setProjectName(SysConf.BLOG);
        fileVO.setSortName(SysConf.ADMIN);
        fileVO.setSystemConfig(JsonUtils.object2Map(systemConfig));
        List<String> urlList = new ArrayList<>();
        if (data.get(SysConf.AVATAR) != null) {
            urlList.add(data.get(SysConf.AVATAR).toString());
        } else if (data.get(SysConf.AVATAR_URL) != null) {
            urlList.add(data.get(SysConf.AVATAR_URL).toString());
        }
        fileVO.setUrlList(urlList);
        String res = this.pictureFeignClient.uploadPicsByUrl(fileVO);
        Map<String, Object> resultMap = JsonUtils.jsonToMap(res);
        if (resultMap.get(com.zy.blog.utils.constant.SysConf.CODE) != null && com.zy.blog.utils.constant.SysConf.SUCCESS.equals(resultMap.get(com.zy.blog.utils.constant.SysConf.CODE).toString())) {
            if (resultMap.get(com.zy.blog.utils.constant.SysConf.DATA) != null) {
                List<Map<String, Object>> listMap = (List<Map<String, Object>>) resultMap.get(com.zy.blog.utils.constant.SysConf.DATA);
                if (listMap != null && listMap.size() > 0) {
                    Map<String, Object> pictureMap = listMap.get(0);

                    String localPictureBaseUrl = systemConfig.getLocalPictureBaseUrl();
                    String qiNiuPictureBaseUrl = systemConfig.getQiNiuPictureBaseUrl();
                    String picturePriority = systemConfig.getPicturePriority();
                    user.setAvatar(pictureMap.get(com.zy.blog.utils.constant.SysConf.UID).toString());
                    // ????????????????????????
                    if (EOpenStatus.OPEN.equals(picturePriority)) {
                        // ???????????????
                        if (pictureMap.get(SysConf.QI_NIU_URL) != null && pictureMap.get(com.zy.blog.utils.constant.SysConf.UID) != null) {
                            user.setPhotoUrl(qiNiuPictureBaseUrl + pictureMap.get(SysConf.QI_NIU_URL).toString());
                        }
                    } else {
                        // ???????????????????????????
                        if (pictureMap.get(SysConf.PIC_URL) != null && pictureMap.get(com.zy.blog.utils.constant.SysConf.UID) != null) {
                            user.setPhotoUrl(localPictureBaseUrl + pictureMap.get(SysConf.PIC_URL).toString());
                        }
                    }
                }
            }
        }
    }

    /**
     * ?????????????????????
     *
     * @param map
     * @return
     */
    @ApiOperation(value = "decryptData", notes = "QQ???????????????????????????")
    @PostMapping("/decryptData")
    public String decryptData(@RequestBody Map<String, String> map) throws UnsupportedEncodingException {

        return null;

    }

    @RequestMapping("/revoke/{source}/{token}")
    public Object revokeAuth(@PathVariable("source") String source, @PathVariable("token") String token) throws IOException {
        return null;
    }

    @RequestMapping("/refresh/{source}")
    public Object refreshAuth(@PathVariable("source") String source, String token) {
        return null;
    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @GetMapping("/verify/{accessToken}")
    public String verifyUser(@PathVariable("accessToken") String accessToken) {
        String userInfo = stringRedisTemplate.opsForValue().get(RedisConf.USER_TOKEN + Constants.SYMBOL_COLON + accessToken);
        if (StringUtils.isEmpty(userInfo)) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.INVALID_TOKEN);
        } else {
            Map<String, Object> map = JsonUtils.jsonToMap(userInfo);
            return ResultUtil.result(SysConf.SUCCESS, map);
        }
    }

    @ApiOperation(value = "??????accessToken", notes = "??????accessToken")
    @RequestMapping("/delete/{accessToken}")
    public String deleteUserAccessToken(@PathVariable("accessToken") String accessToken) {
        return null;
    }

    /**
     * ??????token?????????????????????
     *
     * @param token
     * @return
     */
    @GetMapping("/getSystemConfig")
    public String getSystemConfig(@RequestParam("token") String token) {
        return null;
    }

    /**
     * ????????????????????????
     */
    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @PostMapping("/editUser")
    public String editUser(HttpServletRequest request, @RequestBody UserView userVO) {
        if (request.getAttribute(SysConf.USER_UID) == null || request.getAttribute(SysConf.TOKEN) == null) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.INVALID_TOKEN);
        }
        String userUid = request.getAttribute(SysConf.USER_UID).toString();
        String token = request.getAttribute(SysConf.TOKEN).toString();
        User user = userService.getById(userUid);
        if (user == null) {
            return ResultUtil.result(SysConf.ERROR, "????????????, ??????????????????!");
        }
        log.info("??????????????????: {}", user);
        user.setNickName(userVO.getNickName());
        user.setAvatar(userVO.getAvatar());
        user.setBirthday(userVO.getBirthday());
        user.setSummary(userVO.getSummary());
        user.setGender(userVO.getGender());
        user.setQqNumber(userVO.getQqNumber());
        user.setOccupation(userVO.getOccupation());

        // ??????????????????????????????????????????????????????
        if (userVO.getStartEmailNotification() == SysConf.ONE && !StringUtils.isNotEmpty(user.getEmail())) {
            return ResultUtil.result(SysConf.ERROR, "???????????????????????????????????????????????????????????????~");
        }
        user.setStartEmailNotification(userVO.getStartEmailNotification());
        user.updateById();
        user.setPassWord("");
        user.setPhotoUrl(userVO.getPhotoUrl());

        // ?????????????????????????????????
        if (userVO.getEmail() != null && !userVO.getEmail().equals(user.getEmail())) {
            user.setEmail(userVO.getEmail());
            // ??????RabbitMQ????????????
            rabbitMqUtil.sendRegisterEmail(user, token);
            // ????????????????????????Redis??????????????????
            stringRedisTemplate.opsForValue().set(RedisConf.USER_TOKEN + Constants.SYMBOL_COLON + token, JsonUtils.objectToJson(user), userTokenSurvivalTime, TimeUnit.HOURS);
            return ResultUtil.result(SysConf.SUCCESS, "??????????????????????????????????????????????????????");
        } else {
            stringRedisTemplate.opsForValue().set(RedisConf.USER_TOKEN + Constants.SYMBOL_COLON + token, JsonUtils.objectToJson(user), userTokenSurvivalTime, TimeUnit.HOURS);
            return ResultUtil.result(SysConf.SUCCESS, MessageConf.UPDATE_SUCCESS);
        }
    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @PostMapping("/updateUserPwd")
    public String updateUserPwd(HttpServletRequest request, @RequestParam(value = "oldPwd") String oldPwd, @RequestParam("newPwd") String newPwd) {
        return null;
    }

    @ApiOperation(value = "????????????", notes = "????????????")
    @PostMapping("/replyBlogLink")
    public String replyBlogLink(HttpServletRequest request, @RequestBody LinkView linkVO) {
        return null;

    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @GetMapping("/getFeedbackList")
    public String getFeedbackList(HttpServletRequest request) {
        return null;
    }

    @ApiOperation(value = "????????????", notes = "????????????", response = String.class)
    @PostMapping("/addFeedback")
    public String edit(HttpServletRequest request, @Validated({Insert.class}) @RequestBody FeedBackView feedbackVO, BindingResult result) {

        return null;
    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @GetMapping("/bindUserEmail/{token}/{code}")
    public String bindUserEmail(@PathVariable("token") String token, @PathVariable("code") String code) {
        String userInfo = stringRedisTemplate.opsForValue().get(RedisConf.USER_TOKEN + Constants.SYMBOL_COLON + token);
        if (StringUtils.isEmpty(userInfo)) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.INVALID_TOKEN);
        }
        User user = JsonUtils.jsonToPojo(userInfo, User.class);
        user.updateById();
        return ResultUtil.result(SysConf.SUCCESS, MessageConf.OPERATION_SUCCESS);
    }

    /**
     * ??????
     *
     * @param source
     * @return
     */
    private AuthRequest getAuthRequest(String source) {
        AuthRequest authRequest = null;
        switch (source) {
            case SysConf.GITHUB:
                authRequest = new AuthGithubRequest(AuthConfig.builder()
                        .clientId(githubClienId)
                        .clientSecret(githubClientSecret)
                        .redirectUri(moguWebUrl + "/oauth/callback/github")
                        .build());
                break;
            case SysConf.GITEE:
                authRequest = new AuthGiteeRequest(AuthConfig.builder()
                        .clientId(giteeClienId)
                        .clientSecret(giteeClientSecret)
                        .redirectUri(moguWebUrl + "/oauth/callback/gitee")
                        .build());
                break;
            case SysConf.QQ:
                authRequest = new AuthQqRequest(AuthConfig.builder()
                        .clientId(qqClienId)
                        .clientSecret(qqClientSecret)
                        .redirectUri(moguWebUrl + "/oauth/callback/qq")
                        .build());
                break;
            default:
                break;
        }

        return authRequest;
    }

}
