# aliyun/ros.py
from aliyunsdkcore.client import AcsClient
from aliyunsdkros.request.v20190910 import CreateStackRequest, DescribeStacksRequest

class ROSClient:
    def __init__(self, access_key, secret_key):
        self.client = AcsClient(access_key, secret_key, 'cn-hangzhou')

    def create_stack(self, template):
        request = CreateStackRequest.CreateStackRequest()
        request.set_TemplateBody(template)
        response = self.client.do_action_with_exception(request)
        return response.get('StackId')

    def get_stack_status(self, stack_id):
        request = DescribeStacksRequest.DescribeStacksRequest()
        request.set_StackId(stack_id)
        response = self.client.do_action_with_exception(request)
        return response.get('Stacks')[0].get('Status')