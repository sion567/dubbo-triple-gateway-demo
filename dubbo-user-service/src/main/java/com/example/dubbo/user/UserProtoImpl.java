package com.example.dubbo.user;

import com.example.dubbo.user.mapper.UserMapper;
import com.example.proto.DubboUserProtoServiceTriple;
import com.example.proto.GetUsernamesByUserIdsRequest;
import com.example.proto.GetUsernamesByUserIdsResponse;
import com.example.proto.UserProtoService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@DubboService
//当服务继承 ImplBase 时，Dubbo 使用 StubServiceDescriptor，方法名存储的是 proto 文件中的 UpperCamelCase（如 GetUser），但 Java 实际方法是 lowerCamelCase（如 getUser），导致服务描述符注册异常，进而影响 mapping 的正确写入。
//public class UserProtoImpl extends DubboUserProtoServiceTriple.UserProtoServiceImplBase {
public class UserProtoImpl implements UserProtoService {

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

    @Override
    public CompletableFuture<GetUsernamesByUserIdsResponse> getUsernamesByUserIdsAsync(GetUsernamesByUserIdsRequest request) {
        // 直接复用同步方法即可，无需重复业务逻辑
        return CompletableFuture.completedFuture(getUsernamesByUserIds(request));
    }
}
