package util

import jcuda.driver.{CUcontext, CUdevice, JCudaDriver}
import jcuda.driver.JCudaDriver._

case class Memory(total: Long, free: Long)
/**
  * Created by gary on 1/3/2018.
  */
class Gpu(deviceId: Int) {
  def memory = {
    JCudaDriver.setExceptionsEnabled(true)
    cuInit(0)
    val device: CUdevice = new CUdevice
    JCudaDriver.cuDeviceGet(device, deviceId)

    val total = Array(0L)
    val free = Array(0L)
    cuInit(0)
    cuDeviceGet(device, deviceId)

    val context: CUcontext = new CUcontext
    cuCtxCreate(context, 0, device)
    cuMemGetInfo(free, total)

    val result = Memory(total(0), free(0))

    cuCtxDestroy(context)

    result
  }
}
