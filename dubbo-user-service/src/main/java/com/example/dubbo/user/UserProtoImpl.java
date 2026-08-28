package com.example.dubbo.user;

import com.example.dubbo.user.mapper.UserMapper;
import com.example.proto.DubboUserProtoServiceTriple;
import com.example.proto.GetUsernamesByUserIdsRequest;
import com.example.proto.GetUsernamesByUserIdsResponse;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.Map;

@DubboService
public class UserProtoImpl extends DubboUserProtoServiceTriple.UserProtoServiceImplBase {

    private final UserMapper userMapper;

    public UserProtoImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public GetUsernamesByUserIdsResponse getUsernamesByUserIds(GetUsernamesByUserIdsRequest request) {
        Map<Long, String> nameById = new HashMap<>();
        if (request.getUserIdsCount() > 0) {
            for (Map<String, Object> row : userMapper.selectNamesByIds(request.getUserIdsList())) {
                nameById.put((Long) row.get("id"), (String) row.get("name"));
            }
        }
        GetUsernamesByUserIdsResponse.Builder builder = GetUsernamesByUserIdsResponse.newBuilder();
        // names 与 user_ids 同序；查不到的用户为空串（proto3 无 null，空串即"不存在"的契约）
        for (long id : request.getUserIdsList()) {
            builder.addNames(nameById.getOrDefault(id, ""));
        }
        return builder.build();
    }
}
