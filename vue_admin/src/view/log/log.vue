<template>
  <div class="app-container">
      <!-- 查询和其他操作 -->
	    <div class="filter-container" style="margin: 10px 0 10px 0;">
				<el-input clearable class="filter-item" style="width: 200px;" v-model="queryParams.userName" placeholder="请输入操作人"></el-input>
        <el-input clearable class="filter-item" style="width: 200px;" v-model="queryParams.operation" placeholder="请输入接口名"></el-input>
        <el-input clearable class="filter-item" style="width: 200px;" v-model="queryParams.ip" placeholder="请输入IP"></el-input>

        <el-select v-model="queryParams.spendTime" placeholder="请求耗时" style="width:150px" clearable>
          <el-option
            v-for="item in spendTimeDictList"
            :key="item.uid"
            :label="item.dictLabel"
            :value="item.dictValue"
          ></el-option>
        </el-select>

        <el-date-picker
          clearable
          v-model="value5"
          type="datetimerange"
          :picker-options="pickerOptions2"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          align="right">
        </el-date-picker>

	      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFind" v-permission="'/log/getLogList'">查找</el-button>
	    </div>

      <el-table :data="tableData"  style="width: 100%">

        <el-table-column type="expand">
          <template slot-scope="props">
            <el-form label-position="left" inline class="demo-table-expand">

              <el-row>
                <el-form-item label="请求接口">
                  <span>{{ props.row.classPath + props.row.url }}</span>
                </el-form-item>
              </el-row>
              <el-row>
                <el-form-item label="请求参数">
                  <span>{{ props.row.params }}</span>
                </el-form-item>
              </el-row>
            </el-form>
          </template>
        </el-table-column>

      <el-table-column type="selection"></el-table-column>

      <el-table-column label="序号" width="60" align="center">
	      <template slot-scope="scope">
	        <span >{{scope.$index + 1}}</span>
	      </template>
	    </el-table-column>

	    <el-table-column label="操作人" width="100" align="center">
	      <template slot-scope="scope">
	        <span>{{ scope.row.userName }}</span>
	      </template>
	    </el-table-column>

      <el-table-column label="请求接口" width="150" align="center">
	      <template slot-scope="scope">
	        <span>{{ scope.row.url }}</span>
	      </template>
	    </el-table-column>

      <el-table-column label="请求方式" width="100" align="center">
	      <template slot-scope="scope">
	        <span>{{ scope.row.type }}</span>
	      </template>
	    </el-table-column>

      <el-table-column label="接口名" width="150" align="center">
	      <template slot-scope="scope">
	        <span>{{ scope.row.operation }}</span>
	      </template>
	    </el-table-column>

      <el-table-column label="IP" width="150" align="center">
	      <template slot-scope="scope">
	        <span>{{ scope.row.ip }}</span>
	      </template>
	    </el-table-column>

      <el-table-column label="IP来源" width="200" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.ipSource }}</span>
        </template>
      </el-table-column>

      <!-- <el-table-column label="请求耗时" width="160" align="center">
        <template slot-scope="scope">
          <el-tag v-for="item in spendTimeDictList" :type="item.listClass" :key="item.uid" v-if="scope.row.spendTime >= splitTime(item.dictValue)[0] && scope.row.spendTime < splitTime(item.dictValue)[1]">{{ scope.row.spendTime}} ms </el-tag>
        </template>
      </el-table-column> -->

	    <el-table-column label="创建时间" width="160" align="center">
	      <template slot-scope="scope">
	        <span >{{ scope.row.createTime }}</span>
	      </template>
	    </el-table-column>

	   	<el-table-column label="状态" width="100" align="center">
	   	  <template slot-scope="scope">
		   	  <template v-if="scope.row.status == 1">
		        <span>正常</span>
		      </template>
		      <template v-if="scope.row.status == 2">
		        <span>推荐</span>
		      </template>
		      <template v-if="scope.row.status == 0">
		        <span>已删除</span>
		      </template>
	   	  </template>
	    </el-table-column>

	  </el-table>

    <!--分页-->
    <div class="block">
        <el-pagination
          @current-change="handleCurrentChange"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
    </div>
  </div>
</template>

<script>
import { getLogList } from "@/api/log";
import {getListByDictTypeList} from "@/api/sysDictData"
export default {
  data() {
    return {
      // 查询对象
      queryParams: {
        userName: "",
        operation: "",
        ip: "",
        spendTime: "",
      },
      spendTimeDictList: [],
      tableData: [],
      currentPage: 1,
      pageSize: 10,
      total: 0, //总数量
      formLabelWidth: "120px",
      pickerOptions2: {
        shortcuts: [
          {
            text: "最近一周",
            onClick(picker) {
              const end = new Date();
              const start = new Date();
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
              picker.$emit("pick", [start, end]);
            }
          },
          {
            text: "最近一个月",
            onClick(picker) {
              const end = new Date();
              const start = new Date();
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
              picker.$emit("pick", [start, end]);
            }
          },
          {
            text: "最近三个月",
            onClick(picker) {
              const end = new Date();
              const start = new Date();
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
              picker.$emit("pick", [start, end]);
            }
          }
        ]
      },
      value5: []
    };
  },
  created() {
    this.getDictList()
    this.logList();
  },
  methods: {
    logList: function() {
      var params = {};
      params.userName = this.queryParams.userName;
      params.operation = this.queryParams.operation;
      params.ip = this.queryParams.ip;
      params.spendTimeStr = this.queryParams.spendTime;
      if(this.value5.length >= 2) {
        params.startTime = this.value5[0] + "," + this.value5[1];
      }
      params.currentPage = this.currentPage;
      params.pageSize = this.pageSize;
      getLogList(params).then(response => {
        if(response.code == this.$ECode.SUCCESS) {
          this.currentPage = response.data.current;
          this.pageSize = response.data.size;
          this.total = response.data.total;
          this.tableData = response.data.records;
        }

      });
    },
    splitTime: function (spendTimeStr){
      // 切割耗时区间
      return spendTimeStr.split("_")
    },
    /**
     * 字典查询
     */
    getDictList: function () {
      var dictTypeList =  ['sys_spend_time']
      getListByDictTypeList(dictTypeList).then(response => {
        if (response.code == this.$ECode.SUCCESS) {
          var dictMap = response.data;
          this.spendTimeDictList = dictMap.sys_spend_time.list
        }
      });
    },
    handleFind: function() {
      console.log(this.value5);
      this.logList();
    },
    handleCurrentChange: function(val) {
      console.log("点击了换页");
      this.currentPage = val;
      this.logList();
    }
  }
};
</script>
