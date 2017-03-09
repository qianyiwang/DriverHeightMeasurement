import matplotlib.pyplot as plt
fileName = 'data1'
distance = []
z_v = []
z_acc = []
v_x = []
v_y = []
v_z = []
with open(fileName) as fp:
    for line in fp:
        if 'E/' in line:
            p = line.index(':')
            data = line[p+2:]
            dataArr = data.split(',')
            distance.append(dataArr[0])
            z_v.append(dataArr[1])
            z_acc.append(dataArr[2])
            v_x.append(dataArr[3])
            v_y.append(dataArr[4])
            v_z.append(dataArr[5])
print dataArr

# plot
fig = plt.figure('result')
ax1 = plt.subplot(231)
ax1.set_title('x_acc')
ax1.plot(distance,'bo',distance,'k')
ax2 = plt.subplot(232)
ax2.set_title('y_acc')
ax2.plot(z_v,'go',z_v,'k')
ax3 = plt.subplot(233)
ax3.set_title('z_acc')
ax3.plot(z_acc,'ro',z_acc,'k')
ax4 = plt.subplot(234)
ax4.set_title('v_x')
ax4.plot(v_x,'ro',v_x,'k')
ax5 = plt.subplot(235)
ax5.set_title('v_y')
ax5.plot(v_y,'ro',v_y,'k')
ax6 = plt.subplot(236)
ax6.set_title('v_z')
ax6.plot(v_z,'ro',v_z,'k')

plt.show()