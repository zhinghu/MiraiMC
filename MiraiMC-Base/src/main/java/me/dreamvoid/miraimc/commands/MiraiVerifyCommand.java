package me.dreamvoid.miraimc.commands;

import me.dreamvoid.miraimc.internal.MiraiLoginSolver;

public class MiraiVerifyCommand implements ICommandExecutor {
    @Override
    public boolean onCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("This command should not run directly.");
            return true;
        }

        switch (args[0].toLowerCase()){
            case "unsafedevice":{
                if(args.length >= 2){
                    MiraiLoginSolver.solve(Long.parseLong(args[1]));
                    sender.sendMessage("&a已将验证请求提交到服务器");
                } else sender.sendMessage("&c无效的参数！用法：/miraiverify unsafedevice <账号>");
                break;
            }
            case "captcha":{
                if(args.length >= 3){
                    sender.sendMessage("&a已将验证码提交到服务器");
                    MiraiLoginSolver.solve(Long.parseLong(args[1]),args[2]);
                } else sender.sendMessage("&c无效的参数！用法：/miraiverify captcha <账号> <验证码>");
                break;
            }
            case "cancel":{
                if(args.length >= 2){
                    MiraiLoginSolver.cancel(Long.parseLong(args[1]));
                    sender.sendMessage("&a已取消登录验证流程");
                } else sender.sendMessage("&c无效的参数！用法：/miraiverify cancel <账号>");
                break;
            }
            case "deviceverify":{
                if(args.length >= 2){
                    if(args.length == 2){
                        MiraiLoginSolver.solve(Long.parseLong(args[1]));
                        sender.sendMessage("&a已将验证请求提交到服务器");
                    } else if(args.length == 3){
                        MiraiLoginSolver.solve(Long.parseLong(args[1]), args[2]);
                        sender.sendMessage("&a已将验证码提交到服务器");
                    }
                } else sender.sendMessage("&c无效的参数！用法：/miraiverify unsafedevice <账号>");
                break;
            }
        }
        return true;
    }
}