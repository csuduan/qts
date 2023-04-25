#pragma once
#include "define.h"
#include "sqlite/sqlite3.h"

class SqliteHelper{
private:
    sqlite3 *conn;
    void query(const char * sql,function<void(sqlite3_stmt *)> mapper){
        //const char *sqlSentence = "SELECT name, age FROM t_person WHERE age < 30;";    //SQL语句
        sqlite3_stmt *stmt = NULL;    // stmt语句句柄
        //进行查询前的准备工作——检查语句合法性
        //-1代表系统会自动计算SQL语句的长度
        if (SQLITE_OK == sqlite3_prepare_v2(conn, sql, -1, &stmt, NULL)) {
            // 每调一次sqlite3_step()函数，stmt语句句柄就会指向下一条记录
            while (sqlite3_step(stmt) == SQLITE_ROW) {
                mapper(stmt);
            }
        }
        else {
            loge("invalid sql:{}",sql);
        }
        //清理语句句柄，准备执行下一个语句
        sqlite3_finalize(stmt);
    }
    void execute(const char* sql){
        sqlite3_stmt *stmt = NULL;        //stmt语句句柄
        //进行插入前的准备工作——检查语句合法性
        //-1代表系统会自动计算SQL语句的长度
        if (SQLITE_OK == sqlite3_prepare_v2(conn, sql, -1, &stmt, NULL)) {
            //执行该语句
            sqlite3_step(stmt);
        }
        else {
            loge("invalid sql:{}",sql);
        }
        //清理语句句柄，准备执行下一个语句
        sqlite3_finalize(stmt);
    }

public:
    bool  connected= false;
    SqliteHelper(std::string path="./mts-core.db"){
        int ret= sqlite3_open(path.c_str(), &conn);
        if (ret!=SQLITE_OK){
            loge("Can't open database {}",path);
        }else{
            logi("open database{} ",path);
            connected=true;
        }
    }
    void queryContracts(vector<Contract*> &contracts){
        string sql="SELECT * FROM CONTRACT";
        this->query(const_cast<char *>(sql.c_str()),[&contracts](sqlite3_stmt * stmt){
            Contract* contract=new Contract();
            contract->symbol= (char*)sqlite3_column_text(stmt, 0);
            contract->exchange=(char*)sqlite3_column_text(stmt, 1);
            contract->name=(char*)sqlite3_column_text(stmt, 2);
            //todo
            contracts.push_back(contract);
        });
    }
    void queryBars(vector<Bar*> &bars,string symbol,string level,string startDate){
        char *zErrMsg = 0;
        char* sql="SELECT * FROM BAR";
//        this->query(const_cast<char *>(sql.c_str()),[&bars](sqlite3_stmt * stmt){
//            Bar* bar=new Bar();
//            bar->symbol= (char*)sqlite3_column_text(stmt, 0);
//            //todo
//            bars.push_back(bar);
//        });

        int rc=sqlite3_exec(conn,sql,[](void *param, int colNum, char **col, char **azColName)->int{
                vector<Bar*> *bars =(vector<Bar*> *)param;
                Bar* bar=new Bar();
                bar->symbol= col[0];
                bars->push_back(bar);
                //todo
                return 0;
            },&bars,&zErrMsg);
        if( rc != SQLITE_OK ){
            loge("sql execute eror:{}",zErrMsg);
            sqlite3_free(zErrMsg);
        }

    }
    void saveContracts(vector<Contract *> &contracts){
        char *zErrMsg = 0;
        for(auto & item :contracts){
            char* sql=sqlite3_mprintf("REPLACE INTO CONTRACT VALUES('%s','%s','%s')",
                                      item->symbol.c_str(),
                                      item->exchange.c_str(),
                                      item->name.c_str());
            int rc=sqlite3_exec(conn,sql,NULL,NULL,&zErrMsg);
            if( rc != SQLITE_OK ){
                loge("sql execute eror,{} {}",sql,zErrMsg);
                sqlite3_free(zErrMsg);
            }
        }
    }
    void saveBars(vector<Bar *> &bars){
        char *zErrMsg = 0;
        for(auto & item :bars){
            char* sql=sqlite3_mprintf("INSERT INTO BAR VALUES('%s','%s','%s','%d','%d','%f','%f','%f','%f','%f','%d')",
                                      item->tradingDay.c_str(),
                                      item->symbol.c_str(),
                                      item->exchange.c_str(),
                                      item->level,
                                      item->barTime,
                                      item->updateTime,
                                      item->high,
                                      item->low,
                                      item->open,
                                      item->close,
                                      item->volume);
            int rc=sqlite3_exec(conn,sql,NULL,NULL,&zErrMsg);
            if( rc != SQLITE_OK ){
                loge("sql execute eror,{} {}",sql,zErrMsg);
                sqlite3_free(zErrMsg);
            }else{
                item->saved=true;
            }
        }
    }
};