package org.verwandlung.voj.judger.application;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.verwandlung.voj.judger.mapper.LanguageMapper;
import org.verwandlung.voj.judger.model.Language;

/**
 * 程序测评模块的加载器.
 * 
 * @author Xie Haozhe
 */
public class ApplicationBootstrap {
    /**
     * 应用程序入口.
     */
    public static void main(String[] args) {
        LOGGER.info("Starting Verwandlung Online Judge Judger...");
        ApplicationBootstrap app = new ApplicationBootstrap();
        app.getApplicationContext();
        app.setupHeartBeat();
        app.getSystemEnvironment();
        app.setUpShutdownHook();
        LOGGER.info("Verwandlung Online Judge Judger started.");
    }

    /**
     * 加载应用程序配置.
     */
    private void getApplicationContext() {
        applicationContext = new 
                ClassPathXmlApplicationContext("application-context.xml");
    }
    
    /**
     * 配置与Web模块的心跳连接.
     * 定时向Web模块发送Keep-Alive信号.
     */
    private void setupHeartBeat() {
        final int INITIAL_DELAY = 0;
        final int PERIOD = 25;
        
        ApplicationHeartbeat heartbeat = applicationContext.getBean(ApplicationHeartbeat.class);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(heartbeat, INITIAL_DELAY, PERIOD, TimeUnit.MINUTES);
    }
    
    /**
     * 设置ShutdownHook.
     * 用于完成程序正常退出前的准备工作.
     */
    private void setUpShutdownHook() {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    LOGGER.info("Verwandlung Online Judge Judger is shutting down...");
                    mainThread.join();
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        });
    }
    
    /**
     * 获取系统环境变量.
     * 以便进行Bug的复现.
     */
    private void getSystemEnvironment() {
        LOGGER.info("System Information: " );
        LOGGER.info("\tOperating System Name: " + System.getProperty("os.name"));
        LOGGER.info("\tOperating System Version: " + System.getProperty("os.version"));
        LOGGER.info("\tJava VM Name: " + System.getProperty("java.vm.name"));
        LOGGER.info("\tJava Runtime Version: " + System.getProperty("java.runtime.version"));
        
        LOGGER.info("Compiler Information: " );
        LanguageMapper languageMapper = applicationContext.getBean(LanguageMapper.class);
        List<Language> languages = languageMapper.getAllLanguages();
        for ( Language language : languages ) {
            String languageName = language.getLanguageName();
            String compileProgram = getCompileProgram(language.getCompileCommand());
            LOGGER.info("\t" + languageName + ": " + getCompilerVersion(languageName, compileProgram));   
        }
    }
    
    /**
     * 获取编译程序的命令行.
     * @param compileCommand - 编译命令的命令行
     * @return 编译程序的命令行
     */
    private String getCompileProgram(String compileCommand) {
        int firstSpaceIndex = compileCommand.indexOf(" ");
        String compileProgram = compileCommand.substring(0, firstSpaceIndex);
        
        if ( "javac".equalsIgnoreCase(compileProgram) ) {
            return "java";
        }
        return compileProgram;
    }
    
    /**
     * 获取编译器的版本信息.
     * @param languageName - 编程语言名称
     * @param compileProgram - 编译所使用的命令 
     * @return 编译器的版本信息
     */
    private String getCompilerVersion(String languageName, String compileProgram) {
        String versionCommand = getVersionCommand(languageName);
        StringBuilder compilerVersion = new StringBuilder();
        
        try {
            String command = compileProgram + versionCommand;
            Process process = Runtime.getRuntime().exec(command);
            
            compilerVersion.append("Command Line: " + command + "\n");
            compilerVersion.append(IOUtils.toString(process.getInputStream()));
            compilerVersion.append(IOUtils.toString(process.getErrorStream()));
        } catch ( Exception ex ) {
            return "Not Found";
        }
        return compilerVersion.toString();
    }
    
    /**
     * 获取编译器版本的命令行参数.
     * @param languageName - 编程语言名称
     * @return 获取编译器版本的命令行参数
     */
    private String getVersionCommand(String languageName) {
        if ( "Java".equalsIgnoreCase(languageName) ) {
            return " -version";
        }
        return " --version";
    }
    
    /**
     * 应用程序配置.
     */
    private ApplicationContext applicationContext;
    
    /**
     * 日志记录器.
     */
    private static final Logger LOGGER = LogManager.getLogger(ApplicationBootstrap.class);
}
