package impl;

import com.sun.net.ssl.HttpsURLConnection;
import javafx.util.Pair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.net.www.protocol.http.Handler;
import util.DBHelper;
import java.io.*;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestReptile {

    //总数
    private static int number = 0;

    //存储已经爬取过的网页的url
    private static Set<Integer> mHadDoneUrl = new HashSet<>();

    //存储待爬取的网页url
    private static List<String> mWaitDoneUrl = new ArrayList<>();

    private static final String mRootUrl = "http://www.bj.ganji.com/";

    public static final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(80);

    private static DBHelper mDbHelper = null;
    private static ResultSet mResultSet = null;

    //用来存储城市和对应的URL
    private List<Pair<String, String>> mPairPlaceUrl = new ArrayList<>();

    //用来存储功能和对应的标签规则
    private static List<Pair<String, String>> mPairFeature = new ArrayList<>();

    //保存ip port 到Pair
    private static List<Pair<String, String>> mPairIpPort = new ArrayList<>();

    //网页总数
    private static int mAllPagesNum = 0;

//    private static String sql = "insert into Place_Name_Url(place_name,place_url) value(?,?)";
//    private static String sql = "insert into Features_FromCity(feature) value(?)";
    private static String sql = "insert into zpjisuanjiwangluo(position,company,salary,tag," +
        "place,time,city) value(?,?,?,?,?,?,?)";

    private static String[] mSqls = new String[]{
            "INSERT INTO fang1(title, size, address, price, city) VALUE (?,?,?,?,?)",
            "INSERT INTO fang10(title, listword, frcol2, city) VALUE (?,?,?,?)",
            "INSERT INTO fang11(title, sizes, price, times, word, city) VALUE (?,?,?,?,?,?)",
            "INSERT INTO fang12(title, sizes, address, price, times, city) VALUE (?,?,?,?,?)",
            "INSERT INTO fang5(title, sizes, address, feature, price, times, city) VALUE (?,?,?,?,?,?,?)",
            "INSERT INTO fang6(title, sizes, address, source, price, smallprice, city) VALUE (?,?,?,?,?,?,?)",
            "INSERT INTO fang8(title, listword, frcol2, city) VALUE (?,?,?,?)",
            "INSERT INTO jiaju(t, pricebiao, description, fl, nameadd, city) VALUE (?,?,?,?,?,?)",
            "INSERT INTO qzkefu(basicInfo, specialty, district, salary, city) VALUE (?,?,?,?,?)",
            "INSERT INTO zixingchemaimai(t, pricebiao, description, fl, qqAttest, city) VALUE (?,?,?,?,?,?)"
    };

    //代理ip
    /**
     * 27.54.116.64:80
     94.182.183.35:8080
     41.211.123.206:53281
     187.1.57.1:53281
     85.10.199.117:3128
     122.53.59.194:8080
     180.254.13.139:8080
     194.44.38.174:8080
     176.113.233.0:8080
     177.24.111.127:8080
     */
    private static String[] mDailiIP = new String[]{
            "27.54.116.64:80",
            "94.182.183.35:8080",
            "41.211.123.206:53281",
            "187.1.57.1:53281",
            "85.10.199.117:3128",
            "122.53.59.194:8080",
            "180.254.13.139:8080",
            "194.44.38.174:8080",
            "176.113.233.0:8080",
            "177.24.111.127:8080"
    };


    static final String ip = "forward.xdaili.cn";//这里以正式服务器ip地址为准
    static final int port = 80;//这里以正式服务器端口地址为准

    static int timestamp = (int) (new Date().getTime()/1000);
    //以下订单号，secret参数 须自行改动
    static final String authHeader = authHeaderMD("ZF201710245499yDiSjn", "4d654da9ab1b44c78554829e8321a26f", timestamp);

    public static String authHeaderMD(String orderno, String secret, int timestamp){
        //拼装签名字符串
        String planText = String.format("orderno=%s,secret=%s,timestamp=%d", orderno, secret, timestamp);

        //计算签名
        String sign = org.apache.commons.codec.digest.DigestUtils.md5Hex(planText).toUpperCase();

        //拼装请求头Proxy-Authorization的值
        String authHeader = String.format("sign=%s&orderno=%s&timestamp=%d", sign, orderno, timestamp);
        return authHeader;
    }

    public static void main(String[] args) {
        TestReptile reptile = new TestReptile();

//        //保存ip port
//        reptile.saveIpToDB();

        //获取ip port
        reptile.getIpPortFromDB();

        //获取url，城市名
        reptile.getPlaceUrlFromDB();

        //获取城市的功能项和爬取的标签规则
        reptile.getAllFeaturesFromDB();

        //根据城市的for循环
        reptile.traversalCity();


    }

    /**
     * 获取ip port 从数据库
     */
    private void getIpPortFromDB() {
        PreparedStatement statement = null;
        try {
            statement = DBHelper.getInstance().conn.prepareStatement("SELECT ip,port FROM ips");
            mResultSet = statement.executeQuery();
            while (mResultSet.next()){
                mPairIpPort.add(new Pair<>(mResultSet.getString(1), mResultSet.getString(2)));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("异常55");
        }
    }

    /**
     * 保存ip到数据库
     */
    private void saveIpToDB() {
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File("/home/ningyuwen/图片/ips.txt")));//构造一个BufferedReader类来读取文件
            String s = null;
            while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                result.append(System.lineSeparator()+s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(result.toString());
        String[] ips = result.toString().split(" ");
        for (int i = 0;i < ips.length;i++){
            String ip = ips[i].split(":")[0];
            String port = ips[i].split(":")[1];
            PreparedStatement statement = null;
            try {
                statement = DBHelper.getInstance().conn.prepareStatement("INSERT INTO ips(ip, port) VALUE (?,?)");
                statement.setString(1, ip);
                statement.setString(2, port);
                statement.executeUpdate();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("异常2");
            }
        }

    }

    private static void testDaili(){
        /**
         * 通过代理对象连接
         * @param address
         * @return
         */
        SocketAddress addr = new InetSocketAddress("113.161.180.101", Integer.parseInt("8080"));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
        try{
            URL url = new URL("http://www.baidu.com");
            URLConnection conn = url.openConnection(proxy);
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
            conn.getContent();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getHtml(String address){
        StringBuffer html = new StringBuffer();
        String result = null;
        try{
            URL url = new URL(address);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());

            try{
                String inputLine;
                byte[] buf = new byte[4096];
                int bytesRead = 0;
                while (bytesRead >= 0) {
                    inputLine = new String(buf, 0, bytesRead, "ISO-8859-1");
                    html.append(inputLine);
                    bytesRead = in.read(buf);
                    inputLine = null;
                }
                buf = null;
            }finally{
                in.close();
                conn = null;
                url = null;
            }
            result = new String(html.toString().trim().getBytes("ISO-8859-1"), "gb2312").toLowerCase();

        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally{
            html = null;
        }
        return result;
    }


    /**
     * 获取城市的功能项和爬取的标签规则
     */
    private void getAllFeaturesFromDB() {
        PreparedStatement statement = null;
        try {
            statement = DBHelper.getInstance().conn.prepareStatement("SELECT * FROM Features_FromCity");
            mResultSet = statement.executeQuery();
            while (mResultSet.next()){
                mPairFeature.add(new Pair<>(mResultSet.getString(1), mResultSet.getString(2)));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("异常2");
        }
    }

    /**
     * 遍历城市
     */
    private void traversalCity() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long a = System.currentTimeMillis();
                System.out.println("开始时间： " + a + " " + mPairPlaceUrl.size());

                //采用动态ip, 多线程
                for (int i = 1;i < 100;i++){
                    //每个城市都有固定的功能，暂时10个
                    System.out.println(mPairPlaceUrl.get(i).getKey());
                    traversalFeatures(i);
                }
                fixedThreadPool.shutdown();
                System.out.println("爬取的页面总数为： " + mAllPagesNum);
                System.out.println("爬取总时间： " + (System.currentTimeMillis() - a));

                /**
                 * 更新已爬取的URL总数
                 */
                PreparedStatement statement = null;
                try {
                    PreparedStatement statement1 = DBHelper.getInstance().conn.prepareStatement("SELECT * FROM All_count where id=1");
                    statement = DBHelper.getInstance().conn.prepareStatement("update All_count set all_count=? where id=1");
                    mResultSet = statement1.executeQuery();
                    if(mResultSet.next()){
                        int num = mResultSet.getInt(2);
                        System.out.println(num);
                        statement.setInt(1, mAllPagesNum + num);
                    }
                    statement.executeUpdate();
                    statement.close();
                    statement1.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("异常3");
                }

            }
        }).start();
    }

    /**
     * 遍历每个功能项
     * @param cityPosition 第几个城市
     */
    private void traversalFeatures(int cityPosition) {
//        for (int i = 0;i < mPairFeature.size();i++){
//            //传递cityPosition和 i 下去，组合为待爬取的功能页面的第一个页面
//            //例如：http://bj.ganji.com/zpjisuanjiwangluo/
//            reptileFromUrl(cityPosition, i);
//
//        }

        reptileFromUrl(cityPosition, 7);

    }

    /**
     * 开始爬取
     */
    private void reptileFromUrl(int cityPosition, int featurePosition){
        String cityUrl = mPairPlaceUrl.get(cityPosition).getValue();
        String cityName = mPairPlaceUrl.get(cityPosition).getKey();
        String featureUrl = mPairFeature.get(featurePosition).getKey();
        String host = mPairPlaceUrl.get(cityPosition).getValue().replace("http://","");
        System.out.println("host: " + host);
        System.out.println(cityUrl+featureUrl);

        int n = 30;  //改为多线程之后，不容易设为动态增加URL，所以固定为30页
        for (int i = 1;i < n;i++) {
//            if (getAllContentFromCityLabel(cityName,cityUrl + "zpjisuanjiwangluo/" + "o" + i + "/")){
//            if (getAllContentFromCityLabel1(host, featurePosition, cityName, cityUrl + featureUrl + "o" + i + "/")){
//                n++;
//                try {
//                    //睡眠两秒钟
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }else {
//                break;
//            }

            //添加进线程池
            fixedThreadPool.execute(new ReptileRunnable(host, featurePosition,
                    cityName, cityUrl + featureUrl + "o" + i + "/"));

        }
//        System.out.println("n为： " + n);
    }

    /**
     * 从数据库中获取城市和url
     */
    private void getPlaceUrlFromDB(){
        PreparedStatement statement = null;
        try {
            statement = DBHelper.getInstance().conn.prepareStatement("SELECT * FROM Place_Name_Url");
            mResultSet = statement.executeQuery();
            while (mResultSet.next()){
                mPairPlaceUrl.add(new Pair<>(mResultSet.getString(1), mResultSet.getString(2)));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("异常1");
        }
    }

    /**
     * 获取每个城市的功能
     * @param url url
     */
    private boolean getAllContentFromCityLabel(String city, String url){
        try {
            Document doc = Jsoup.connect(url)
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                    .header("Host","deyang.ganji.com")
                    .header("Connection", "keep-alive")
                    .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Encoding","gzip, deflate")
                    .timeout(5000)
                    .get();

            /**
             * 获取 elements,匹配规则的第一条
             */
            Elements elements = doc.body().select("dl[class=list-noimg job-list clearfix new-dl]");
            if (elements.size() == 0){
                //return false 表示不再继续爬取这一功能的子页
                return false;
            }

            /**
             * 获取element中的有效信息,并存储
             */
            for (Element element:elements){
                PreparedStatement statement = null;
                try {
                    statement = DBHelper.getInstance().conn.prepareStatement(sql);
                    statement.setString(1, element.select("a[class=list_title gj_tongji]").get(0).text());
                    statement.setString(2, element.select("a[target=_blank]").get(1).text());
                    statement.setString(3, element.select("div[class=new-dl-salary]").get(0).text());
                    statement.setString(4, element.select("div[class=new-dl-tags]").get(0).text());
                    statement.setString(5, element.select("dd[class=pay]").get(0).text());
                    statement.setString(6, element.select("dd[class=pub-time]").get(0).text());
                    statement.setString(7, city);
                    statement.executeUpdate();
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("异常");
                    return false;
                }
            }

            //保存Url
            saveUrlToDB(url);
            mAllPagesNum += 1;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 代理ip获取html内容
     * @param href
     */
    public static Document getDocByJsoup(String href, String host){
        int max = mPairIpPort.size()-1;
        int min = 0;
        Random random = new Random();
        int s = random.nextInt(max)%(max-min+1) + min;
        System.out.println("sdedaxa  " + s + " " + mPairIpPort.get(s).getKey() + " " + mPairIpPort.get(s).getValue());

        String ip = mPairIpPort.get(s).getKey();
        int port = Integer.valueOf(mPairIpPort.get(s).getValue());

        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));

            URL url = new URL(null, href, new Handler());
            HttpURLConnection urlcon = (HttpURLConnection)url.openConnection(proxy);
            urlcon.setConnectTimeout(30000);  //30秒
            urlcon.setRequestProperty("Connection", "keep-alive");
            urlcon.setRequestProperty("Host", host);
            urlcon.setRequestProperty("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.62 Safari/537.36");
            urlcon.connect();         //获取连接
            InputStream is = urlcon.getInputStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
            StringBuffer bs = new StringBuffer();
            String l = null;
            while((l=buffer.readLine())!=null){
                bs.append(l);
            }
            return Jsoup.parse(bs.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * runnable 可以提供参数
     */
    private static class ReptileRunnable implements Runnable{
        String host;
        int featurePosition;
        String city;
        String url;

        public ReptileRunnable(String host, int featurePosition, String city, String url) {
            super();
            this.host = host;
            this.featurePosition = featurePosition;
            this.city = city;
            this.url = url;
        }

        @Override
        public void run() {
            System.out.println("查看： " + url);
            getAllContentFromCityLabel1(host, featurePosition, city, url);
        }
    }

    /**
     * 获取每个城市的功能
     * @param url url
     */
    private static boolean getAllContentFromCityLabel1(String host, int featurePosition, String city, String url){
        try {
//            Document doc = Jsoup.connect(url)
//                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
//                    .header("Host", host)
//                    .header("Connection", "keep-alive")
//                    .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                    .header("Accept-Encoding","gzip, deflate")
//                    .timeout(5000)
//                    .get();

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
            Document doc = Jsoup.connect(url)
                    .proxy(proxy)
                    .validateTLSCertificates(false) //忽略证书认证,每种语言客户端都有类似的API
                    .header("Proxy-Authorization", authHeader)
                    .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                    .header("Host", host)
                    .header("Connection", "keep-alive")
                    .header("Accept-Encoding","gzip, deflate")
                    .get();

//            Document doc = getDocByJsoup(url, host);
//            if (doc == null){
//                return false;
//            }

            /**
             * 解析匹配规则
             */
//            System.out.println(mPairFeature.get(featurePosition).getValue().toString());
            String[] features = mPairFeature.get(featurePosition).getValue().split("1234");
//            System.out.println(features.length + " : " + features[0]);


            /**
             * 获取 elements,匹配规则的第一条
             */
            Elements elements = doc.body().select(features[0].split("=")[0] + "[class=" +
                    features[0].split("=")[1] + "]");
            if (elements.size() == 0){
                //return false 表示不再继续爬取这一功能的子页
                return false;
            }

            /**
             * 获取element中的有效信息,并存储
             */
            for (Element element:elements){
                PreparedStatement statement = null;
                try {
                    statement = DBHelper.getInstance().conn.prepareStatement(mSqls[featurePosition]);
                    for (int i = 1;i < features.length;i++){
                        //从一开始
                        String feature = features[i].split("=")[0] + "[class=" + features[i].split("=")[1] + "]";
                        String saveText = "";

//                        if ("".equals(element.select(feature).get(0).text())){
//                            //某些标签内容在第二个里
////                        System.out.println(element.select(feature).get(1).text());
//                            saveText = element.select(feature).get(1).text();
//                        }else {
//                            saveText = element.select(feature).get(0).text();
//                        }

                        saveText = element.select(feature).get(0).text();
                        statement.setString(i, saveText);

//                        System.out.println(saveText);

                    }
                    statement.setString(features.length, city);
                    statement.executeUpdate();
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.out.println("插入数据异常");
                    return false;
                }
            }

            //保存Url
            saveUrlToDB(url);
            mAllPagesNum += 1;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("访问异常，ip");
        }
        return true;
    }

    /**
     * 保存已经爬取的Url
     * @param url
     */
    private static void saveUrlToDB(String url){
        /**
         * 将成功爬取的url存储起来
         */
        try {
            PreparedStatement statement1 = DBHelper.getInstance().conn.prepareStatement(
                    "insert into Count_Url(reptile_url) value(?)");
            statement1.setString(1, url);
            statement1.executeUpdate();
            statement1.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void testNode(){
        MyNode<String> root = new MyNode<String>("A",-1);
        MyTree<String> tree = new MyTree<String>(root);
        MyNode<String> b = new MyNode<String>("B");
        MyNode<String> c = new MyNode<String>("C");
        MyNode<String> d = new MyNode<String>("D");
        MyNode<String> e = new MyNode<String>("E");
        MyNode<String> f = new MyNode<String>("F");
        MyNode<String> g = new MyNode<String>("G");
        tree.add(b,root);
        tree.add(c,root);
        tree.add(d,root);

        tree.add(e,b);
        tree.add(f,b);
        tree.add(g,f);


        System.out.println(tree.getSize());
        System.out.println(tree.getRoot().getData());
//        System.out.println(tree.getAllNodes());
        System.out.println(tree.getDepth());
        tree.add(new MyNode<String>("H"),g);
        System.out.println(tree.getDepth());
        tree.enlarge();
    }

    /**
     * 获取当前host页面全部url
     * urls 存储待爬取的url ，使用List存储，爬了之后将List<String>中删掉，添加到mHadDoneUrl中
     */
    private static void getAllUrlFromHtml(String htmlUrl){
        try {
            Document doc = Jsoup.connect(htmlUrl)
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36")
                    .header("Host","deyang.ganji.com")
                    .header("Connection", "keep-alive")
                    .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Encoding","gzip, deflate")
                    .timeout(8000)
                    .get();

            //选择div下的city
//            Elements elements = doc.select("div[class=all-city]");
//            elements = elements.select("dd");


//            System.out.println(doc.childNodes());
//            System.out.println(doc.body().getAllElements());

//            for (Element element : doc.body().children()) {
//                for (Element element1 : element.children()){
//                    System.out.println(element1.nodeName());
//                }
////                System.out.println(element.nodeName());
//            }

//            for (Element element:doc.body().children().get(0).children()){
//                System.out.println(element.nodeName());
//            }

            Elements nodeElements = doc.body().siblingElements();
            while (nodeElements.size() != 0){
                for (Element element : nodeElements){
                    System.out.println(element.nodeName());
                }
                nodeElements = nodeElements.next();
                System.out.println("\n");
            }


//            List<DataNode> nodeList = doc.dataNodes();
//            for (DataNode node:nodeList){
//                System.out.println(node.nodeName());
//            }

//            System.out.println(elements.size());

//            try {
//
//                statement.setString(1, "李小龙2hao");
//                statement.setString(2, "s/fsfa/sa/000");
//                statement.executeUpdate();
//                statement.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }

//            try {
//                for (int i =0;i < elements.size();i++){
//                    Elements everyOne = elements.get(i).getElementsByTag("a");
//                    for (int j = 0; j < everyOne.size();j++){
//                        number++;
//                        PreparedStatement statement = DBHelper.getInstance().conn.prepareStatement(sql);
//                        statement.setString(1, everyOne.get(j).text());
//                        statement.setString(2, everyOne.get(j).attr("href"));
//                        statement.executeUpdate();
//                        statement.close();
//                    }
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }


            //现在改存数据库
//            //将文件内容存储到文件
//            try {
//                htmlUrl = htmlUrl.replaceFirst(mRootUrl,"");
//                File file = new File("/home/ningyuwen/Android/BackStage/Reptile/reptile/" + htmlUrl.hashCode() + ".txt");
//                if (!file.exists()){
//                    file.createNewFile();
//                }
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                OutputStreamWriter outputWriter = new OutputStreamWriter(fileOutputStream,"UTF-8");
//
//                for (int i =0;i < elements.size();i++){
//                    System.out.println(elements.get(i).getElementsByTag("tr").text());
//
//                    if (elements.get(i).getElementsByTag("tr").text().equals("")){
//                        file.delete();
//                        return;
//                    }
//                    fileOutputStream.write((elements.get(i).getElementsByTag("tr").text() + "\n").getBytes());
//                    number++;
//                }
//
////                String str = doc.text();
////                fileOutputStream.write(str.getBytes());
//                outputWriter.close();
//                fileOutputStream.close();
//            }catch(IOException e) {
//                e.printStackTrace();
//            }

//            //获取html文件中的链接
//            Elements links = doc.getElementsByTag("a");
//            System.out.println("连接数：  " + links.size());
//
//            for (Element link: links) {
//                String linkHref = link.attr("href");
//                System.out.println(linkHref);
//                mWaitDoneUrl.add(linkHref);
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private static int getNodeFromHtml(Element element, int size){
//        if (size == 0){
//            return 0;
//        }else {
//            return size + getNodeFromHtml(element.children().size());
//        }
//    }

    /**
     * Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/
    /*Accept-Encoding:gzip, deflate, br
    Accept-Language:zh-CN,zh;q=0.8
    Cache-Control:max-age=0
    Connection:keep-alive
    Content-Length:112
    Content-Type:application/x-www-form-urlencoded
    Cookie:uuid_tt_dd=-1257727158487465017_20170925; _ga=GA1.2.1452563296.1506333079; UM_distinctid=15ec84d50e14fa-0628420649c87d-3970065f-1fa400-15ec84d50e294b; __utma=17226283.1452563296.1506333079.1507542765.1507542765.1; __utmz=17226283.1507542765.1.1.utmcsr=blog.lanyus.com|utmccn=(referral)|utmcmd=referral|utmcct=/archives/174.html; __message_sys_msg_id=0; __message_gu_msg_id=0; __message_cnel_msg_id=0; __message_district_code=000000; __message_in_school=0; UN=in_clude; UE=""; BT=1508161943621; JSESSIONID=842E9CFAB8969EC9A12A5AFE6A3116B0.tomcat1; LSSC=LSSC-754167-nu4Pg3atZ3qyWcuTOYJwG1LYUNyWYZ-passport.csdn.net; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1508081458,1508081934,1508082641,1508134309; Hm_lpvt_6bcd52f51e9b3dce32bec4a3997715ac=1508162004; dc_tos=oxx590; dc_session_id=1508134308988_0.10622958924640713
    Host:passport.csdn.net
    Origin:https://passport.csdn.net
    Referer:https://passport.csdn.net/account/login?from=http%3A%2F%2Fmy.csdn.net%2Fmy%2Fmycsdn
    Upgrade-Insecure-Requests:1
    User-Agent:Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36
     */
    private String sendPost(String url, Map<String, String> param){
        Connection con = Jsoup.connect(url + "?http://my.csdn.net/my/mycsdn");
        con.data("username", param.get("username"));
        con.data("password", param.get("password"));
        con.data("lt", param.get("lt"));
        con.data("execution", param.get("execution"));
        con.data("_eventId", param.get("_eventId"));

        Document doc = null;
        try {
            doc = con.post();
            // 将获取到的内容打印出来
            System.out.println(doc.location());
            System.out.println(doc.body().text());
            return doc.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 拼接完整的formData
     * @param hiddenData
     * @return
     */
    private Map<String, String> getFromData(Map<String, String> hiddenData){
        hiddenData.put("username", "15681223110");
        hiddenData.put("password", "ningyuwen0619");
        return hiddenData;
    }

    /**
     * 获取hidden参数列表
     * @param url url
     * @return string
     */
    private Map<String, String> getFormDataFromUrl(String url){
        Map<String, String> formData = new HashMap<>();
        Document doc = null;
        try {
            doc = Jsoup.connect(url)
                    //.data("query", "Java")
                    //.userAgent("头部")
                    //.cookie("auth", "token")
                    //.timeout(3000)
                    //.post()
//                    .header("Host", "passport.csdn.net")
                    .get();

            //hidden标签下的三个参数
            Elements elements = doc.select("input[type=hidden]");
            String formDataStr = "";
            for (Element element : elements){
                System.out.println(element.attr("name") + "=" + element.attr("value"));
                formDataStr = formDataStr + "&" + element.attr("name") + "=" + element.attr("value");
                formData.put(element.attr("name"), element.attr("value"));

            }
            return formData;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("exception");
        }
        return new HashMap<>();
    }

}
