package com.viiup.android.flock.application;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.Iconify;
import com.viiup.android.flock.helpers.CommonHelper;
import com.viiup.android.flock.models.UserGroupModel;
import com.viiup.android.flock.services.IAsyncRequestResponse;
import com.viiup.android.flock.services.UserService;

import java.util.List;

/**
 * Created by AbdullahMoyeen on 4/12/16.
 */
public class HomeGroupsCellAdapter extends BaseAdapter {

    private static class CellItemsViewHolder {
        ImageView imageViewGroup;
        TextView textViewGroupCategory;
        TextView textViewGroupName;
        TextView textViewGroupMembersCount;
        TextView textViewGroupDescription;
        Switch switchMembership;
    }

    private Context context;
    private ListView listView;
    private List<UserGroupModel> userGroups;
    private CellItemsViewHolder cellItemsViewHolder;
    private ProgressDialog progressDialog;

    HomeGroupsCellAdapter(Context context, ListView listView, List<UserGroupModel> userGroups) {
        this.context = context;
        this.listView = listView;
        this.userGroups = userGroups;
    }

    @Override
    public int getCount() {
        return userGroups.size();
    }

    @Override
    public Object getItem(int position) {
        return userGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final UserGroupModel userGroup = userGroups.get(position);

        if (convertView == null) {

            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.home_groups_cell, null);

            cellItemsViewHolder = new CellItemsViewHolder();
            cellItemsViewHolder.imageViewGroup = (ImageView) convertView.findViewById(R.id.imageViewGroup);
            cellItemsViewHolder.textViewGroupCategory = (TextView) convertView.findViewById(R.id.textViewGroupCategory);
            cellItemsViewHolder.textViewGroupName = (TextView) convertView.findViewById(R.id.textViewGroupName);
            cellItemsViewHolder.textViewGroupMembersCount = (TextView) convertView.findViewById(R.id.textViewGroupMembersCount);
            cellItemsViewHolder.textViewGroupDescription = (TextView) convertView.findViewById(R.id.textViewGroupDescription);
            cellItemsViewHolder.switchMembership = (Switch) convertView.findViewById(R.id.switchMembership);

            convertView.setTag(cellItemsViewHolder);
        } else {
            cellItemsViewHolder = (CellItemsViewHolder) convertView.getTag();
        }

        if (userGroup != null) {

            cellItemsViewHolder.imageViewGroup.setImageDrawable(CommonHelper.getIconDrawableByGroupCategory(this.context, userGroup.group.getGroupCategory()));
            cellItemsViewHolder.textViewGroupCategory.setText(userGroup.group.getGroupCategory());
            cellItemsViewHolder.textViewGroupName.setText(userGroup.group.getGroupName());
            cellItemsViewHolder.textViewGroupMembersCount.setText(Integer.toString(userGroup.group.getActiveMemberCount()) + " joined");
            cellItemsViewHolder.textViewGroupDescription.setText(userGroup.group.getGroupDescription());
            cellItemsViewHolder.switchMembership.setTextOff(Iconify.compute(context, context.getString(R.string.fa_icon_leave)));
            if (userGroup.getGroupMembershipStatus().equals("P")) {
                cellItemsViewHolder.switchMembership.setTextOn(Iconify.compute(context, context.getString(R.string.fa_icon_pending)));
                cellItemsViewHolder.switchMembership.setEnabled(false);
            } else {
                cellItemsViewHolder.switchMembership.setTextOn(Iconify.compute(context, context.getString(R.string.fa_icon_join)));
                cellItemsViewHolder.switchMembership.setEnabled(true);
            }
            cellItemsViewHolder.switchMembership.setOnCheckedChangeListener(null);
            cellItemsViewHolder.switchMembership.setChecked(!userGroup.getGroupMembershipStatus().equals("I"));
            cellItemsViewHolder.switchMembership.setOnCheckedChangeListener(new SwitchMembershipOnCheckedChangeListener());
        }

        return convertView;
    }

    private class SwitchMembershipOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener, IAsyncRequestResponse {

        private boolean isMember;
        private int position;

        @Override
        public void responseHandler(String response) {

            if (progressDialog != null) progressDialog.dismiss();

            if (response.equalsIgnoreCase("OK")) {
                if (isMember) {
                    Toast.makeText(context, R.string.msg_join_request_sent, Toast.LENGTH_SHORT).show();
                    int pendingMemberCount = userGroups.get(position).group.getPendingMemberCount();
                    userGroups.get(position).setGroupMembershipStatus("P");
                    userGroups.get(position).group.setPendingMemberCount(pendingMemberCount + 1);
                    cellItemsViewHolder.switchMembership.setTextOn("PEN");
                    cellItemsViewHolder.switchMembership.setEnabled(false);
                } else {
                    int activeMemberCount = userGroups.get(position).group.getActiveMemberCount();
                    userGroups.get(position).setGroupMembershipStatus("I");
                    userGroups.get(position).group.setActiveMemberCount(activeMemberCount - 1);
                }

                listView.setAdapter(listView.getAdapter());
            } else {
                Toast.makeText(context, R.string.msg_processing_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void backGroundErrorHandler(Exception ex) {

            if (progressDialog != null) progressDialog.dismiss();

            // Print stack trace...may be add logging in future releases
            ex.printStackTrace();

            // display error message
            Toast.makeText(context, R.string.msg_something_wrong, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {

            this.isMember = isOn;
            this.position = listView.getPositionForView(buttonView);

            // Display progress bar
            progressDialog = ProgressDialog.show(context, "MEMBERSHIP", buttonView.getResources().getString(R.string.msg_processing_request));

            UserService userService = new UserService();
            userService.setUserGroupMembership(userGroups.get(position).getUserId(), userGroups.get(position).group.getGroupId(), isOn, this);
        }
    }
}
