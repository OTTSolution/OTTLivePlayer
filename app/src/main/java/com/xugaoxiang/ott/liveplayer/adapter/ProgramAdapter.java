package com.xugaoxiang.ott.liveplayer.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xugaoxiang.ott.liveplayer.activity.GetServiceProgramList;
import com.xugaoxiang.ott.liveplayer.activity.MainActivity;
import com.xugaoxiang.ott.liveplayer.bean.LiveBean;
import com.xugaoxiang.ott.liveplayer.R;


/**
 * Created by user on 2016/10/10.
 */
public class ProgramAdapter extends BaseAdapter{
    private Context context;

    public ProgramAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return MainActivity.liveBean.getData().size();
    }

    @Override
    public LiveBean.DataBean getItem(int position) {
        return MainActivity.liveBean.getData().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null){
            convertView = View.inflate(context , R.layout.lv_program_item , null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if (!TextUtils.isEmpty(GetServiceProgramList.language)&&GetServiceProgramList.language.equals("英文")){
            viewHolder.textView.setText(TextUtils.isEmpty(getItem(position).getNum())
                    ?getItem(position).getEn_name()
                    :getItem(position).getNum()+"    "+getItem(position).getEn_name());
        }else {
            viewHolder.textView.setText(TextUtils.isEmpty(getItem(position).getNum())
                    ?getItem(position).getName()
                    :getItem(position).getNum()+"    "+getItem(position).getName());
        }
        return convertView;
    }

    static class ViewHolder{

        private final TextView textView;

        public ViewHolder(View view) {
            textView = (TextView) view.findViewById(R.id.tv_program_name);
        }
    }
}