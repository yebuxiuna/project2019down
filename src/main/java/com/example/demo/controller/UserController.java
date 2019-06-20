package com.example.demo.controller;

import com.example.demo.dao.ApplyinfoDao;
import com.example.demo.dao.UserDao;
import com.example.demo.dao.UserinfoDao;
import com.example.demo.entry.ApplyInfo;
import com.example.demo.entry.UserPhone;
import com.example.demo.entry.UserRegister;
import com.example.demo.result.ResultVO;
import com.example.demo.result.ResultVOUtil;
import com.example.demo.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Data")
public class UserController {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserinfoDao userinfoDao;
    @Autowired
    private ApplyinfoDao applyinfoDao;

    /**
     * 用户注册
     * @param map 用户输入的信息，需要有token,phone，username，pwd，email
     * @return 返回一个json
     */
    @RequestMapping(value = "/Register",method = {RequestMethod.POST})
    public ResultVO register(@RequestBody Map map){
        ResultVO resultVO = null;
        if(map.get("token") != null){
            resultVO = new ResultVO();
            UserPhone user = userDao.findByPhone(map.get("phone").toString());
            if(user != null){
                return ResultVOUtil.error("该用户已存在");
            }
            UserPhone tuser = new UserPhone();
            tuser.setPhone(map.get("phone").toString());
            UserPhone user2=userDao.save(tuser);
            resultVO.setMsg("S");
            WebSocketServer wss = WebSocketServer.getWebSocketServer(map.get("token").toString());
            wss.setSid(user.getId());
            resultVO.setData(user2);
            System.out.print(user2);
            UserRegister info = new UserRegister();
            info.setId(user2.getId());
            info.setUsername(map.get("username").toString());
            info.setPwd(map.get("pwd").toString());
            info.setToken(map.get("token").toString());
            String email = map.get("email").toString();
            info.setEmail(email != null ? email : null);
            userinfoDao.save(info);
            return resultVO;
        }else{
            return ResultVOUtil.error("error：no token");
        }
    }

    /**
     * 用户登录
     * @param map
     * @return
     */
    @RequestMapping(value = "/Login",method = {RequestMethod.POST})
    public ResultVO login(@RequestBody Map map){
        ResultVO resultVO = null;
        if(map.get("token") != null){
            resultVO = new ResultVO();
            UserPhone userPhone = userDao.findByPhone(map.get("phone").toString());
            if(userPhone == null){
                return ResultVOUtil.error("该用户不存在");
            }
            UserRegister userRegister = userinfoDao.findById(userPhone.getId()).get();
            if(!map.get("pwd").equals(userRegister.getPwd())){
                return ResultVOUtil.error("密码错误！");
            }
            WebSocketServer wss = WebSocketServer.getWebSocketServer(map.get("session").toString());
            wss.setSid(userRegister.getId());
            //向我发送好友申请的用户id
            ArrayList<Integer> fids = new ArrayList<>();
            List<ApplyInfo> applyInfos = applyinfoDao.findAllByFid(userRegister.getId());
            for (int i = 0; i < applyInfos.size(); i++) {
                fids.add(applyInfos.get(i).getUid());
            }
            if(fids.size() != 0){
                receiveFriend(map,fids);
            }
            resultVO.setMsg("S");
            ArrayList al = new ArrayList();
            al.add(userPhone);
            al.add(userRegister);
            resultVO.setData(al);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    map.put("state",1);
                    UpdateSate(map);
                }
            }).start();
            return resultVO;
        }else{
            return ResultVOUtil.error("error：no token");
        }
    }

    /**
     * 登录时将未在线时收到的申请发送到客户端
     * 通过WebSocket让客户端接收好友申请
     * @param map 装载着用户数据的集合
     * @param ids 用户id
     */
    public void receiveFriend(Map map,ArrayList<Integer> ids){
        WebSocketServer wss = WebSocketServer.getWebSocketServer(map.get("token").toString());
        for (int i = 0; i < ids.size(); i++) {
            UserRegister userRegister = userinfoDao.findById(ids.get(i)).get();
            wss.receiveApply(userRegister.getUsername(),ids.get(i));
        }
    }

    /**
     * 是否同意添加好友
     * 用户登录的session token
     * 用户是否同意 result
     * 用户自己的id uid
     * 申请人的id fid
     * @param map
     */
    @RequestMapping(value = "/AgreementAdd",method = {RequestMethod.POST})
    public void Succeedfriend(@RequestBody Map map){
        if(map.get("token") != null){
            if(map.get("result").equals("yes")){
                WebSocketServer wss = WebSocketServer.getWebSocketServer(map.get("token").toString());

            }
            ApplyInfo applyInfo = new ApplyInfo();
            applyInfo.setUid((Integer) map.get("fid"));
            applyInfo.setFid((Integer) map.get("uid"));
            applyinfoDao.delete(applyInfo);
        }
    }

    /**
     * 添加好友
     * @param map
     * @return
     */
    @RequestMapping(value = "/AddFriend" , method = {RequestMethod.POST})
    public  ResultVO addFriend(@RequestBody Map map){
        if(map.get("token") != null){
            ResultVO resultVO = new ResultVO();
            ApplyInfo applyInfo = applyinfoDao.selectApply((Integer) map.get("uid"),(Integer) map.get("fid"));
            if(applyInfo != null){
                return ResultVOUtil.error("已发送申请");
            }
            ApplyInfo applyInfo2 = new ApplyInfo();
            applyInfo2.setUid((Integer) map.get("uid"));
            applyInfo2.setFid((Integer) map.get("fid"));
            ApplyInfo applyInfo3 = applyinfoDao.save(applyInfo2);
            UserPhone userPhone = userDao.findById((Integer) map.get("fid")).get();
            UserRegister userRegister = userinfoDao.findById((Integer) map.get("uid")).get();
            if(applyInfo3 != null){
                if(userPhone.getToken() != null){
                    WebSocketServer wss = WebSocketServer.getWebSocketServer(userPhone.getToken());
                    wss.sendApply(wss,userRegister.getUsername(),(Integer) map.get("uid"));
                }
                resultVO.setMsg("S");
                resultVO.setData(applyInfo3);
                return resultVO;
            }else{
                return ResultVOUtil.error("请求发送失败");
            }
        }
        return ResultVOUtil.error("error:no token");
    }

    /**
     * 修改登录状态
     * @param map
     */
    public void UpdateSate(Map map){
        userinfoDao.setState((Integer)map.get("state"),(Integer)map.get("id"),map.get("token").toString());
    }

    /**
     * 用户退出
     * @param map
     */
    @RequestMapping(value = "/Exit",method = {RequestMethod.POST})
    public void exit(@RequestBody Map map){
        if(map.get("token") != null){
            map.put("state",0);
            map.put("token",null);
            UpdateSate(map);
        }
    }

    /**
     * 修改密码 or 忘记密码
     * @param map
     * @return
     */
    @RequestMapping(value = "/UpdatePwd" , method = {RequestMethod.POST})
    public ResultVO updatePwd(@RequestBody Map map){
        if(map.get("token") != null){
            int num = userinfoDao.updatePwd(map.get("pwd").toString(), (Integer) map.get("id"));
            ResultVO resultVO = new ResultVO();
            if(num > 0){
                resultVO.setMsg("S");
                resultVO.setData("修改成功！");
            }else{
                resultVO.setMsg("F");
                resultVO.setData("修改失败！");
            }
            return resultVO;
        }
        return ResultVOUtil.error("error:no token");
    }

}