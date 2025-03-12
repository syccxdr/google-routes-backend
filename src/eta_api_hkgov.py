import time
from operator import index
import requests
from pandas.core.interchange.from_dataframe import primitive_column_to_ndarray
import pandas as pd


def get_kmb_eta(stop_id):
    """
    调用巴士到站信息 API，获取 ETA（预计到达时间）。

    :param stop_id: 巴士站 ID (16 个字符)
    :param route: 巴士路线编号
    :param service_type: 服务类型
    :return: 返回的 ETA 时间列表
    """
    # API 基础 URL
    base_url = "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta"  # 替换为实际 API 的域名

    # 构造完整的 URL
    url = f"{base_url}/{stop_id}"
    try:
        # 发起 GET 请求
        response = requests.get(url)

        # 检查 HTTP 状态码
        if response.status_code == 200:
            # 解析 JSON 数据
            data = response.json()
            eta_list = []

            # 提取 ETA 信息（假设在 `data` 键中）
            for item in data.get("data", []):
                eta = item.get("eta")  # 假设 ETA 时间在 `eta` 键中
                if eta:
                    eta_list.append(eta)

            return eta_list
        else:
            print(f"请求失败，HTTP 状态码: {response.status_code}")
            return []

    except Exception as e:
        print(f"请求出现错误: {e}")
        return []


def get_citybus_eta(stop_id, route):
    url = f"https://rt.data.gov.hk/v2/transport/citybus/eta/CTB/{stop_id}/{route}"  # 替换为实际 API 的域名
    try:
        # 发起 GET 请求
        response = requests.get(url)
        eta_list = []
        # 检查 HTTP 状态码
        if response.status_code == 200:
            # 解析 JSON 数据
            data = response.json()
            for item in data.get("data", []):
                eta = item.get("eta")  # 假设 ETA 时间在 `eta` 键中
                if eta:
                    eta_list.append(eta)
            return eta_list
        else:
            print(f"请求失败，HTTP 状态码: {response.status_code}")
            return []

    except Exception as e:
        print(f"请求出现错误: {e}")
        return []

def get_bus_eta(stop_id,route_id,company):
    if company == 'kmb':
        eta = get_kmb_eta(stop_id)
    elif company == 'cb':
        eta = get_citybus_eta(stop_id,route_id)
    else:
        eta = []
    print(eta)
    return eta
# 示例调用
if __name__ == "__main__":
    # 示例参数 - cb
    # stop_id = "002403"
    # route_id = "1"
    # company_id = 'cb'

    stop_id = "18492910339410B1"
    route_id = "1"
    company_id = 'kmb'
    eta = get_bus_eta(stop_id,route_id,company_id)

