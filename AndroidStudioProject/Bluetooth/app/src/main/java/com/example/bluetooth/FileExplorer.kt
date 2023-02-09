package com.example.bluetooth

import android.Manifest
import android.Manifest.permission
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import java.io.File


class FileExplorer{
    //현재 Directory
    var m_CurFilePath : String? = null
    var m_Root : String? = null
    var m_aFiles : ArrayList<String>? = null


    companion object {
        var t_strfilePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    var MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1
    var TAG : String? = null

    init{
        //log의 TAG
        TAG = "FileExplorer"

        //File Array List
        m_aFiles = ArrayList<String>()

        m_Root = Environment.getExternalStorageDirectory().absolutePath
        m_CurFilePath = m_Root
        //m_Adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, m_aFiles)
    }

    fun SetUpPath(){
        val end: Int = m_CurFilePath!!.lastIndexOf("/") // /가 나오는 마지막 인덱스를 찾고
        val uppath: String = m_CurFilePath!!.substring(0, end) //그부분을 짤라버림 즉 위로가게됨
        m_CurFilePath = uppath
    }

    fun IsPathRoot(): Boolean{
        return m_CurFilePath == m_Root
    }

    fun SetPathRoot(){
        m_CurFilePath = m_Root
    }

    fun SetCurFilePath(path : String){
        m_CurFilePath = path
    }

    fun GetCurFilePath(): String? {
        return m_CurFilePath
    }

    fun GetFileList(): ArrayList<String>?{
        return m_aFiles
    }


    fun RefreshFiles(){
        m_aFiles!!.clear()
        val current = File(m_CurFilePath as String) //현재 경로로 File클래스를 만듬
        val files: Array<String> = current.list() as Array<String>//현재 경로의 파일과 폴더 이름을 문자열 배열로 리턴
        if(files.isEmpty() == false)
        {
            //여기서 출력을 해줌
            for (i in 0 until files.size)
            {
                val Path: String = m_CurFilePath + "/" + files[i]
                var Name = ""
                val f = File(Path)
                Name = if (f.isDirectory) {
                    "[" + files[i] + "]" //디렉토리면 []를 붙여주고

                } else {
                    files[i] //파일이면 그냥 출력
                }
                Log.d("hskang", Name)
                m_aFiles!!.add(Name) //배열리스트에 추가해줌
            }
        }
    }
}


/*



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);



        mCurrentTxt = (TextView)findViewById(R.id.current);

        mFileList = (ListView)findViewById(R.id.filelist);



        arFiles = new ArrayList();

        //SD카드 루트 가져옴

        mRoot = Environment.getExternalStorageDirectory().getAbsolutePath();

        mCurrent = mRoot;



        //어댑터를 생성하고 연결해줌

        mAdapter = new ArrayAdapter(this,

        android.R.layout.simple_list_item_1, arFiles);

        mFileList.setAdapter(mAdapter);//리스트뷰에 어댑터 연결

        mFileList.setOnItemClickListener(mItemClickListener);//리스너 연결



        refreshFiles();

        //권한허용
        checkPermission();

    }



    //리스트뷰 클릭 리스너

    AdapterView.OnItemClickListener mItemClickListener =

    new AdapterView.OnItemClickListener() {



        @SuppressLint("WrongConstant")
        @Override

        public void onItemClick(AdapterView<?> parent, View view,

            int position, long id) {

            // TODO Auto-generated method stub

            String Name = arFiles.get(position);//클릭된 위치의 값을 가져옴



            //디렉토리이면

            if(Name.startsWith("[") && Name.endsWith("]")){

                Name = Name.substring(1, Name.length() - 1);//[]부분을 제거해줌

            }

            //들어가기 위해 /와 터치한 파일 명을 붙여줌

            String Path = mCurrent + "/" + Name;

            File f = new File(Path);//File 클래스 생성

            if(f.isDirectory()){//디렉토리면?

                mCurrent = Path;//현재를 Path로 바꿔줌

                refreshFiles();//리프레쉬

            }else{//디렉토리가 아니면 토스트 메세지를 뿌림

                Toast.makeText(FileExplorer.this, arFiles.get(position), 0).show();

            }

        }

    };



    //버튼 2개 클릭시

    public void mOnClick(View v){

        switch(v.getId()){

            case R.id.btnroot://루트로 가기

            if(mCurrent.compareTo(mRoot) != 0){//루트가 아니면 루트로 가기

                mCurrent = mRoot;

                refreshFiles();//리프레쉬

            }

            break;

            case R.id.btnup:

            if(mCurrent.compareTo(mRoot) != 0){//루트가 아니면

                int end = mCurrent.lastIndexOf("/");///가 나오는 마지막 인덱스를 찾고

                String uppath = mCurrent.substring(0, end);//그부분을 짤라버림 즉 위로가게됨

                mCurrent = uppath;

                refreshFiles();//리프레쉬

            }

            break;

        }

    }





    void refreshFiles(){

        mCurrentTxt.setText(mCurrent);//현재 PATH를 가져옴

        arFiles.clear();//배열리스트를 지움

        File current = new File(mCurrent);//현재 경로로 File클래스를 만듬

        String[] files = current.list();//현재 경로의 파일과 폴더 이름을 문자열 배열로 리턴



        //파일이 있다면?

        if(files != null){

            //여기서 출력을 해줌

            for(int i = 0; i < files.length;i++){

                String Path = mCurrent + "/" + files[i];

                String Name = "";



                File f = new File(Path);

                if(f.isDirectory()){

                    Name = "[" + files[i] + "]";//디렉토리면 []를 붙여주고

                }else{

                    Name = files[i];//파일이면 그냥 출력

                }



                arFiles.add(Name);//배열리스트에 추가해줌

            }

        }

        //다끝나면 리스트뷰를 갱신시킴

        mAdapter.notifyDataSetChanged();

    }

    public void checkPermission(){
        // 접근권한이 없을때(저장공간)
        if(PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // 최초 권한 요청인지, 혹은 사용자에 의한 재요청인지 확인
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // 사용자가 임의로 권한을 취소한 경우
                // 권한 재요청
                Log.i(TAG, "권한 재요청");
                requestPermissions(permission, MY_PERMISSIONS_REQUEST_READ_CONTACTS);


            }else {
                // 최초로 권한을 요청하는 경우(첫실행)
                Log.i(TAG, "권한 최초요청");
                requestPermissions(permission, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }

        } else { // 접근권한이 있을때
            Log.i(TAG, "접근 허용");
        }

    }

    // 권한체크
//onRequestPermissionsResult는 사용자가 (허용, 거절) 버턴을 눌렀는지 판별하기위해 존재
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:
            // 권한 허용
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "권한 허용");
                Toast.makeText(getApplicationContext(),"허용되었습니다", Toast.LENGTH_LONG).show();
            }else { //권한 허용 불가
                Log.i(TAG, "권한 거절");
                Toast.makeText(getApplicationContext(),"앱권한설정하세요", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }
*/


