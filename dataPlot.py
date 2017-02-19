import matplotlib.pyplot as plt
fileName = 'data1'
distance = []
z_v = []
z_acc = []
with open(fileName) as fp:
    for line in fp:
        if 'E/' in line:
            p = line.index(':')
            data = line[p+2:]
            dataArr = data.split(',')
            distance.append(dataArr[0])
            z_v.append(dataArr[1])
            z_acc.append(dataArr[2])


# plot
fig = plt.figure('result')
ax1 = plt.subplot(131)
ax1.set_title('distance')
ax1.plot(distance,'bo',distance,'k')
ax2 = plt.subplot(132)
ax2.set_title('z_v')
ax2.plot(z_v,'go',z_v,'k')
ax3 = plt.subplot(133)
ax3.set_title('z_acc')
ax3.plot(z_acc,'ro',z_acc,'k')

plt.show()